package ca.umika.api.printing;

import ca.umika.api.order.OrderResponse;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static ca.umika.api.printing.PrinterDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Runs against a disposable PostgreSQL database, never against application tables. */
@EnabledIfEnvironmentVariable(named="UMIKA_PRINT_TEST_JDBC_URL", matches=".+")
class PrinterServiceIntegrationTest {
    private DriverManagerDataSource source;
    private JdbcTemplate db;
    private PrinterService service;
    private String schema;
    private UUID location, other, order, printer, instance;
    private String token;
    @BeforeEach void setup() {
        String url=System.getenv("UMIKA_PRINT_TEST_JDBC_URL");
        var admin=new JdbcTemplate(new DriverManagerDataSource(url,System.getProperty("user.name"),""));
        schema="print_test_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE SCHEMA "+schema);
        source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,System.getProperty("user.name"),"");
        db=new JdbcTemplate(source);
        db.execute("CREATE TABLE locations(id UUID PRIMARY KEY,name TEXT DEFAULT 'Test store')");
        db.execute("CREATE TABLE orders(id UUID PRIMARY KEY,status VARCHAR(30))");
        db.execute("CREATE TABLE users(id UUID PRIMARY KEY,phone TEXT)");
        db.execute("CREATE TABLE menu_categories(id UUID PRIMARY KEY,location_id UUID,is_deleted BOOLEAN DEFAULT FALSE)");
        db.execute("CREATE TABLE menu_items(id UUID PRIMARY KEY,category_id UUID,location_id UUID,is_deleted BOOLEAN DEFAULT FALSE)");
        db.execute("CREATE TABLE roles(id UUID PRIMARY KEY,name TEXT)");
        db.execute("CREATE TABLE system_menus(id UUID PRIMARY KEY DEFAULT gen_random_uuid(),parent_id UUID,name TEXT,name_en TEXT,name_zh TEXT,name_ko TEXT,code TEXT UNIQUE,path TEXT,component TEXT,icon TEXT,menu_type TEXT,sort_order INT)");
        db.execute("CREATE TABLE role_menus(role_id UUID,menu_id UUID,PRIMARY KEY(role_id,menu_id))");
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V44__printer_queue.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V45__agent_owned_printer_addresses.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V46__printer_item_routing.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V47__printer_category_routing.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V48__always_print_whole_order.sql")).execute(source);
        new ResourceDatabasePopulator(new FileSystemResource("../../SQL/V49__whole_and_station_tickets_on_same_printer.sql")).execute(source);
        service=new PrinterService(db,JsonMapper.builder().findAndAddModules().build());
        location=UUID.randomUUID(); other=UUID.randomUUID(); order=UUID.randomUUID(); instance=UUID.randomUUID(); printer=UUID.randomUUID();
        db.update("INSERT INTO locations(id) VALUES (?),(?)",location,other);
        db.update("INSERT INTO orders VALUES (?,'PAID')",order);
        service.save(location,new Settings(3,30,true,List.of(new Printer(printer,"Kitchen",true))));
        token=service.pair(location).token();
    }
    @AfterEach void cleanup() { if(db!=null) db.execute("DROP SCHEMA "+schema+" CASCADE"); }
    private OrderResponse paidOrder() {
        OrderResponse value=mock(OrderResponse.class);
        when(value.id()).thenReturn(order); when(value.locationId()).thenReturn(location); when(value.orderNumber()).thenReturn("TEST-1");
        when(value.items()).thenReturn(List.of()); return value;
    }
    private PollResponse poll() { return service.poll(token,new Poll(instance,Map.of())); }
    @Test void itemRoutingAndWholeOrderSettingAreFrozenForAllTicketReprints() {
        UUID reception=UUID.randomUUID(), sushi=UUID.randomUUID(), soupItem=UUID.randomUUID(), rollItem=UUID.randomUUID();
        db.update("INSERT INTO menu_items(id,location_id) VALUES (?,?),(?,?)",soupItem,location,rollItem,location);
        List<Printer> printers=List.of(new Printer(printer,"Back kitchen",true),new Printer(sushi,"Sushi bar",true),new Printer(reception,"Reception",true));
        service.saveRouting(location,new Routing(printers,reception,false,List.of(new ItemRoute(soupItem,printer),new ItemRoute(rollItem,sushi))));
        var value=paidOrder();
        when(value.items()).thenReturn(List.of(
            new OrderResponse.OrderItemResponse(UUID.randomUUID(),soupItem,"Soup",1,null,null,null,Map.of("note","No onion")),
            new OrderResponse.OrderItemResponse(UUID.randomUUID(),rollItem,"Roll",2,null,null,null,Map.of("note","No wasabi"))));
        service.enqueue(value); service.enqueue(value);
        var initial=poll().jobs();
        assertEquals(3,initial.size());
        assertEquals(2,((List<?>)initial.stream().filter(j->j.printerId().equals(reception)).findFirst().orElseThrow().receipt().get("items")).size());
        for(var job:initial.stream().filter(j->!j.printerId().equals(reception)).toList()) {
            assertEquals(1,((List<?>)job.receipt().get("items")).size());
            assertEquals(Boolean.TRUE,job.receipt().get("stationTicket"));
            assertFalse(job.receipt().containsKey("customerPhone"));
        }
        for(var job:initial) {
            service.acknowledge(token,job.id(),new Ack(instance,"SENT"));
        }
        service.saveRouting(location,new Routing(printers,reception,true,List.of()));
        service.reprintOrder(location,order);
        var reprints=poll().jobs();
        assertEquals(3,reprints.size());
        assertTrue(reprints.stream().allMatch(Job::reprint));
        assertEquals(2,((List<?>)reprints.stream().filter(j->j.printerId().equals(reception)).findFirst().orElseThrow().receipt().get("items")).size());
        assertEquals(1,((List<?>)reprints.stream().filter(j->j.printerId().equals(printer)).findFirst().orElseThrow().receipt().get("items")).size());
        assertThrows(ResponseStatusException.class,()->service.reprintOrder(location,order));
    }
    @Test void categoryRoutingAppliesToNewItemsAndIndividualExceptionsWin() {
        UUID category=UUID.randomUUID(), first=UUID.randomUUID(), addedLater=UUID.randomUUID(), special=UUID.randomUUID(), reception=UUID.randomUUID();
        db.update("INSERT INTO menu_categories(id,location_id) VALUES (?,?)",category,location);
        db.update("INSERT INTO menu_items(id,category_id,location_id) VALUES (?,?,?),(?,?,?)",first,category,location,special,category,location);
        var printers=List.of(new Printer(printer,"Kitchen",true),new Printer(reception,"Reception",true));
        service.saveRouting(location,new Routing(printers,reception,false,List.of(new ItemRoute(special,reception)),List.of(new CategoryRoute(category,printer))));
        db.update("INSERT INTO menu_items(id,category_id,location_id) VALUES (?,?,?)",addedLater,category,location);
        var value=paidOrder();
        when(value.items()).thenReturn(List.of(
            new OrderResponse.OrderItemResponse(UUID.randomUUID(),first,"First",1,null,null,null,null),
            new OrderResponse.OrderItemResponse(UUID.randomUUID(),addedLater,"New",1,null,null,null,null),
            new OrderResponse.OrderItemResponse(UUID.randomUUID(),special,"Special",1,null,null,null,null)));
        service.enqueue(value);
        var jobs=poll().jobs();
        assertEquals(3,jobs.size());
        assertEquals(2,((List<?>)jobs.stream().filter(j->j.printerId().equals(printer)).findFirst().orElseThrow().receipt().get("items")).size());
        assertEquals(2,jobs.stream().filter(j->j.printerId().equals(reception)).count());
        assertEquals(1,((List<?>)jobs.stream().filter(j->Boolean.TRUE.equals(j.receipt().get("stationTicket"))&&j.printerId().equals(reception)).findFirst().orElseThrow().receipt().get("items")).size());
        service.reprintOrder(location,order);
        assertEquals(3,poll().jobs().stream().filter(Job::reprint).count());
        assertThrows(ResponseStatusException.class,()->service.reprintOrder(location,order));
    }
    @Test void wholeOrderSnapshotsCustomerAndPaymentDetailsForReprint() {
        UUID customer=UUID.randomUUID(), itemId=UUID.randomUUID(), kitchen=UUID.randomUUID();
        db.update("INSERT INTO users(id,phone) VALUES (?,?)",customer,"416-555-0123");
        db.update("INSERT INTO menu_items(id,location_id) VALUES (?,?)",itemId,location);
        service.saveRouting(location,new Routing(List.of(new Printer(printer,"Reception",true),new Printer(kitchen,"Kitchen",true)),printer,false,List.of(new ItemRoute(itemId,kitchen))));
        var value=paidOrder();
        when(value.userId()).thenReturn(customer);
        when(value.customerName()).thenReturn("Alex Chen");
        when(value.createdAt()).thenReturn(LocalDateTime.of(2026,9,16,18,2));
        when(value.requestedPickupTime()).thenReturn(LocalDateTime.of(2026,9,16,18,30));
        when(value.subtotal()).thenReturn(new BigDecimal("26.50"));
        when(value.taxAmount()).thenReturn(new BigDecimal("3.45"));
        when(value.tipAmount()).thenReturn(new BigDecimal("4.00"));
        when(value.finalTotal()).thenReturn(new BigDecimal("33.95"));
        when(value.items()).thenReturn(List.of(new OrderResponse.OrderItemResponse(UUID.randomUUID(),itemId,"Roll",2,new BigDecimal("8.50"),null,new BigDecimal("17.00"),null)));
        service.enqueue(value);
        var jobs=poll().jobs();
        assertEquals(2,jobs.size());
        var whole=jobs.stream().filter(j->j.printerId().equals(printer)).findFirst().orElseThrow().receipt();
        assertEquals("416-555-0123",whole.get("customerPhone"));
        assertEquals("Alex Chen",whole.get("customerName"));
        assertEquals("2026-09-16T18:02",whole.get("placedAt"));
        assertEquals(4.0,((Number)whole.get("tip")).doubleValue());
        var station=jobs.stream().filter(j->j.printerId().equals(kitchen)).findFirst().orElseThrow().receipt();
        assertEquals("Alex Chen",station.get("customerName"));
        assertEquals(33.95,((Number)station.get("finalTotal")).doubleValue());
        for(String key:List.of("customerPhone","placedAt","subtotal","tax","tip")) assertFalse(station.containsKey(key));
        for(var job:jobs) service.acknowledge(token,job.id(),new Ack(instance,"SENT"));
        db.update("UPDATE users SET phone=? WHERE id=?","416-555-9999",customer);
        service.reprintOrder(location,order);
        assertEquals("416-555-0123",poll().jobs().stream().filter(j->j.printerId().equals(printer)).findFirst().orElseThrow().receipt().get("customerPhone"));
    }
    @Test void routingValidationAndAgentCannotOverwriteManagers() {
        UUID foreignItem=UUID.randomUUID();
        db.update("INSERT INTO menu_items(id,location_id) VALUES (?,?)",foreignItem,other);
        var printers=List.of(new Printer(printer,"Reception",true));
        assertThrows(ResponseStatusException.class,()->service.saveRouting(location,new Routing(printers,printer,true,List.of(new ItemRoute(foreignItem,printer)))));
        assertThrows(ResponseStatusException.class,()->service.saveRouting(location,new Routing(List.of(new Printer(printer,"Reception",true),new Printer(UUID.randomUUID()," reception ",true)),printer,true,List.of())));
        service.saveRouting(location,new Routing(printers,printer,false,List.of()));
        service.configure(token,new Configure(instance,new Settings(7,30,true,List.of(new Printer(printer,"Wrong name",false)))));
        assertEquals("Reception",service.configuration(location).printers().getFirst().name());
        assertTrue(service.configuration(location).printers().getFirst().enabled());
        assertTrue(service.configuration(location).autoWholeOrder());
    }
    @Test void unassignedItemsUseWholeOrderEvenWhenAutomaticCopyIsOff() {
        UUID item=UUID.randomUUID();
        var value=paidOrder();
        when(value.items()).thenReturn(List.of(new OrderResponse.OrderItemResponse(UUID.randomUUID(),item,"Unassigned",1,null,null,null,null)));
        service.saveRouting(location,new Routing(List.of(new Printer(printer,"Reception",true)),printer,false,List.of()));
        service.enqueue(value);
        assertEquals(1,poll().jobs().size());
        assertEquals(Boolean.TRUE,poll().jobs().getFirst().receipt().get("wholeOrder"));
    }
    @Test void defaultPrinterGetsWholeAndItsOwnStationTicketForOneAssignedItem() {
        UUID salmon=UUID.randomUUID();
        db.update("INSERT INTO menu_items(id,location_id) VALUES (?,?)",salmon,location);
        service.saveRouting(location,new Routing(List.of(new Printer(printer,"Sushi bar",true)),printer,true,List.of(new ItemRoute(salmon,printer))));
        var value=paidOrder();
        when(value.customerName()).thenReturn("Alex Chen");
        when(value.items()).thenReturn(List.of(new OrderResponse.OrderItemResponse(UUID.randomUUID(),salmon,"Salmon sushi",1,new BigDecimal("9.50"),null,new BigDecimal("9.50"),Map.of("note","No wasabi"))));
        service.enqueue(value);
        var jobs=poll().jobs();
        assertEquals(2,jobs.size());
        assertEquals(Boolean.TRUE,jobs.getFirst().receipt().get("wholeOrder"));
        assertEquals(Boolean.TRUE,jobs.get(1).receipt().get("stationTicket"));
        assertTrue(jobs.stream().allMatch(j->j.printerId().equals(printer)));
        assertEquals(1,jobs.stream().filter(j->Boolean.TRUE.equals(j.receipt().get("wholeOrder"))).count());
        assertEquals(1,jobs.stream().filter(j->Boolean.TRUE.equals(j.receipt().get("stationTicket"))).count());
        assertEquals("Alex Chen",jobs.stream().filter(j->Boolean.TRUE.equals(j.receipt().get("stationTicket"))).findFirst().orElseThrow().receipt().get("customerName"));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM print_jobs WHERE order_id=?",Integer.class,order));
        for(var job:jobs) service.acknowledge(token,job.id(),new Ack(instance,"SENT"));
        service.reprintOrder(location,order);
        assertEquals(2,poll().jobs().stream().filter(Job::reprint).count());
    }
    @Test void agentReprintsAreScopedAndCannotDuplicatePendingWork() {
        service.enqueue(paidOrder());
        UUID id=poll().jobs().getFirst().id();
        service.acknowledge(token,id,new Ack(instance,"SENT"));
        String otherToken=service.pair(other).token();
        assertThrows(ResponseStatusException.class,()->service.agentReprint(otherToken,id,new Reprint(UUID.randomUUID())));
        service.agentReprint(token,id,new Reprint(instance));
        assertThrows(ResponseStatusException.class,()->service.agentReprint(token,id,new Reprint(instance)));
        assertEquals(1,poll().jobs().size());
        assertTrue(poll().jobs().getFirst().reprint());
    }
    @Test void repeatsAndLostAcknowledgementsDoNotCreateDuplicateJobs() {
        service.enqueue(paidOrder()); service.enqueue(paidOrder());
        var first=poll(); assertEquals(1,first.jobs().size());
        UUID id=first.jobs().getFirst().id(); assertEquals(id,poll().jobs().getFirst().id());
        service.acknowledge(token,id,new Ack(instance,"SENT"));
        service.acknowledge(token,id,new Ack(instance,"SENT"));
        assertTrue(poll().jobs().isEmpty()); assertEquals(1,service.jobs(location).size());
    }
    @Test void repeatedPaymentAfterUpgradePreservesLegacyReceipts() {
        service.enqueue(paidOrder());
        UUID original=poll().jobs().getFirst().id();
        service.acknowledge(token,original,new Ack(instance,"SENT"));
        // Simulate a receipt created before V46 introduced frozen ticket snapshots.
        db.update("DELETE FROM print_order_tickets WHERE order_id=?",order);
        UUID reception=UUID.randomUUID();
        service.saveRouting(location,new Routing(List.of(new Printer(printer,"Kitchen",true),new Printer(reception,"Reception",true)),reception,true,List.of()));
        service.enqueue(paidOrder());
        assertEquals(1,service.jobs(location).size());
        assertTrue(poll().jobs().isEmpty());
        service.reprintOrder(location,order);
        var reprints=poll().jobs();
        assertEquals(1,reprints.size());
        assertEquals(printer,reprints.getFirst().printerId());
        assertTrue(reprints.getFirst().reprint());
    }
    @Test void paymentRollbackAlsoRollsBackPrintJob() {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        assertThrows(IllegalStateException.class,()->tx.execute(status->{service.enqueue(paidOrder()); throw new IllegalStateException("payment rollback");}));
        assertTrue(service.jobs(location).isEmpty());
    }
    @Test void supportsFourPrintersAndLocationIsolation() {
        var printers=new ArrayList<Printer>();
        for(int i=1;i<=4;i++) printers.add(new Printer(UUID.randomUUID(),"Printer "+i,true));
        service.save(location,new Settings(7,30,true,printers)); service.enqueue(paidOrder());
        assertEquals(4,poll().jobs().size()); assertEquals(7,poll().configuration().pollSeconds());
        String otherToken=service.pair(other).token();
        assertTrue(service.poll(otherToken,new Poll(UUID.randomUUID(),Map.of())).jobs().isEmpty());
        UUID job=service.jobs(location).getFirst().id();
        assertThrows(ResponseStatusException.class,()->service.acknowledge(otherToken,job,new Ack(UUID.randomUUID(),"SENT")));
        assertThrows(ResponseStatusException.class,()->service.save(other,new Settings(3,30,true,List.of(new Printer(printer,"Bad",true)))));
    }
    @Test void pairingRotationNeverReplaysClaimedJobs() {
        service.enqueue(paidOrder()); assertEquals(1,poll().jobs().size());
        assertThrows(ResponseStatusException.class,()->service.poll(token,new Poll(UUID.randomUUID(),Map.of())));
        String old=token; token=service.pair(location).token();
        assertThrows(ResponseStatusException.class,()->service.poll(old,new Poll(instance,Map.of())));
        assertTrue(poll().jobs().isEmpty()); assertEquals("UNCERTAIN",service.jobs(location).getFirst().state());
    }
    @Test void holdsStaleAndCancelledJobsAndMakesExplicitReprints() {
        service.enqueue(paidOrder());
        db.update("UPDATE print_jobs SET created_at=NOW()-INTERVAL '31 minutes'");
        assertTrue(poll().jobs().isEmpty()); var held=service.jobs(location).getFirst(); assertEquals("HELD",held.state());
        service.reprint(location,held.id()); assertTrue(poll().jobs().getFirst().reprint());
        assertThrows(ResponseStatusException.class,()->service.reprint(location,held.id()));
        db.update("UPDATE orders SET status='CANCELLED'");
        assertThrows(ResponseStatusException.class,()->service.reprint(location,held.id()));
    }
    @Test void outstandingWorkIsBoundedPerPrinter() {
        UUID secondPrinter=UUID.randomUUID();
        service.save(location,new Settings(3,30,true,List.of(new Printer(printer,"Kitchen",true),new Printer(secondPrinter,"Counter",true))));
        for(int i=0;i<14;i++) {
            UUID nextOrder=UUID.randomUUID(); db.update("INSERT INTO orders VALUES (?,'PAID')",nextOrder);
            var value=paidOrder(); when(value.id()).thenReturn(nextOrder); service.enqueue(value);
        }
        assertEquals(20,poll().jobs().size()); assertEquals(20,poll().jobs().size());
        assertEquals(10,poll().jobs().stream().filter(j->j.printerId().equals(secondPrinter)).count());
    }
    @Test void agentRegistersLogicalPrintersWithoutIpAddresses() throws Exception {
        var settings=new Settings(8,25,true,List.of(new Printer(printer,"Sushi counter",true)));
        var config=service.configure(token,new Configure(instance,settings));
        assertEquals(location,config.locationId()); assertEquals("Test store",config.locationName());
        assertEquals(8,config.pollSeconds()); assertEquals("Kitchen",config.printers().getFirst().name());
        assertNull(db.queryForObject("SELECT ip_address FROM order_printers WHERE id=?",String.class,printer));
        assertFalse(JsonMapper.builder().findAndAddModules().build().writeValueAsString(config).contains("ipAddress"));
        assertThrows(ResponseStatusException.class,()->service.configure(token,new Configure(UUID.randomUUID(),settings)));
        assertThrows(ResponseStatusException.class,()->service.poll("invalid",new Poll(instance,Map.of())));
    }
}
