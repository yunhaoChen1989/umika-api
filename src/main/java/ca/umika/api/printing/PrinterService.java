package ca.umika.api.printing;

import ca.umika.api.order.OrderResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static ca.umika.api.printing.PrinterDtos.*;

@Service
@Transactional
public class PrinterService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public PrinterService(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }
    public Map<String,String> health() { db.queryForObject("SELECT 1",Integer.class); return Map.of("service","umika-printing","status","ok"); }
    private void ensure(UUID location) {
        db.update("INSERT INTO printer_settings(location_id) VALUES (?) ON CONFLICT DO NOTHING", location);
    }
    private void lock(UUID location) {
        ensure(location);
        db.queryForObject("SELECT location_id FROM printer_settings WHERE location_id=? FOR UPDATE", UUID.class, location);
    }
    public Configuration configuration(UUID location) {
        ensure(location);
        var printers=db.query("SELECT * FROM order_printers WHERE location_id=? ORDER BY name,id",
            (r,n)->new Printer(r.getObject("id",UUID.class),r.getString("name"),r.getBoolean("enabled")),location);
        String locationName=db.queryForObject("SELECT name FROM locations WHERE id=?",String.class,location);
        return db.queryForObject("SELECT * FROM printer_settings WHERE location_id=?", (r,n)->new Configuration(
            location,locationName,r.getInt("poll_seconds"),r.getInt("stale_minutes"),r.getBoolean("alarm_enabled"),printers,
            r.getTimestamp("last_seen")==null?null:r.getTimestamp("last_seen").toInstant(),r.getString("agent_token_hash")!=null,
            parse(r.getString("agent_status")),r.getBoolean("routing_managed"),r.getObject("whole_order_printer_id",UUID.class),
            r.getBoolean("auto_whole_order"),readRoutes(r.getString("item_routes")),readCategoryRoutes(r.getString("category_routes"))),location);
    }
    public Configuration save(UUID location, Settings settings) {
        lock(location);
        Set<UUID> existingIds=new HashSet<>();
        for (Printer p:configuration(location).printers()) existingIds.add(p.id());
        long newPrinters=settings.printers().stream().filter(p->p.id()==null || !existingIds.contains(p.id())).count();
        if (existingIds.size()+newPrinters>16) bad("PRINT_MAX_PRINTERS");
        Set<UUID> ids=new HashSet<>();
        for (Printer p:settings.printers()) {
            UUID id=p.id()==null?UUID.randomUUID():p.id();
            if (!ids.add(id)) bad("PRINT_DUPLICATE_PRINTER");
            var existing=db.queryForList("SELECT location_id FROM order_printers WHERE id=?",UUID.class,id);
            if (!existing.isEmpty() && !location.equals(existing.getFirst())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            db.update("""
                INSERT INTO order_printers(id,location_id,name,enabled) VALUES (?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name,enabled=EXCLUDED.enabled
                """,id,location,p.name().trim(),p.enabled());
        }
        // Explicitly disable rather than delete: preserve job history and printer identity.
        for (Printer p:configuration(location).printers()) {
            if (!ids.contains(p.id())) db.update("UPDATE order_printers SET enabled=false WHERE id=?",p.id());
        }
        db.update("UPDATE printer_settings SET poll_seconds=?,stale_minutes=?,alarm_enabled=? WHERE location_id=?",
            settings.pollSeconds(),settings.staleMinutes(),settings.alarmEnabled(),location);
        return configuration(location);
    }
    public Configuration configure(String token, Configure request) {
        UUID location=authenticate(token,request.instanceId());
        // Logical identities belong to managers. Older agents may still include
        // their cached printers, which must never overwrite manager routing.
        db.update("UPDATE printer_settings SET poll_seconds=?,stale_minutes=?,alarm_enabled=? WHERE location_id=?",
            request.settings().pollSeconds(),request.settings().staleMinutes(),request.settings().alarmEnabled(),location);
        return configuration(location);
    }
    private List<ItemRoute> readRoutes(String value) {
        try { return json.readValue(value,new TypeReference<List<ItemRoute>>(){}); }
        catch(Exception e) { throw new IllegalStateException("Invalid printer routing",e); }
    }
    private List<CategoryRoute> readCategoryRoutes(String value) {
        try { return json.readValue(value,new TypeReference<List<CategoryRoute>>(){}); }
        catch(Exception e) { throw new IllegalStateException("Invalid printer category routing",e); }
    }
    public Configuration saveRouting(UUID location, Routing request) {
        lock(location);
        Map<UUID,Printer> printers=new HashMap<>();
        Set<String> names=new HashSet<>();
        for(Printer p:request.printers()) {
            if(printers.put(p.id(),p)!=null || p.name().isBlank() || !names.add(p.name().trim().toLowerCase(Locale.ROOT)))
                bad("PRINT_DUPLICATE_PRINTER");
        }
        Printer whole=printers.get(request.wholeOrderPrinterId());
        if(whole==null || !whole.enabled()) bad("PRINT_WHOLE_ORDER_REQUIRED");
        Set<UUID> items=new HashSet<>();
        for(ItemRoute route:request.itemRoutes()) {
            Printer target=printers.get(route.printerId());
            if(target==null || !target.enabled() || !items.add(route.menuItemId())) bad("PRINT_INVALID_ROUTE");
            Boolean allowed=db.queryForObject("SELECT EXISTS(SELECT 1 FROM menu_items WHERE id=? AND (location_id IS NULL OR location_id=?) AND is_deleted IS NOT TRUE)",
                Boolean.class,route.menuItemId(),location);
            if(!Boolean.TRUE.equals(allowed)) bad("PRINT_INVALID_MENU_ITEM");
        }
        Set<UUID> categories=new HashSet<>();
        for(CategoryRoute route:request.categoryRoutes()) {
            Printer target=printers.get(route.printerId());
            if(target==null || !target.enabled() || !categories.add(route.categoryId())) bad("PRINT_INVALID_ROUTE");
            Boolean allowed=db.queryForObject("SELECT EXISTS(SELECT 1 FROM menu_categories WHERE id=? AND (location_id IS NULL OR location_id=?) AND is_deleted IS NOT TRUE)",
                Boolean.class,route.categoryId(),location);
            if(!Boolean.TRUE.equals(allowed)) bad("PRINT_INVALID_MENU_CATEGORY");
        }
        Configuration old=configuration(location);
        save(location,new Settings(old.pollSeconds(),old.staleMinutes(),old.alarmEnabled(),request.printers()));
        db.update("UPDATE printer_settings SET routing_managed=true,whole_order_printer_id=?,auto_whole_order=?,item_routes=?::jsonb,category_routes=?::jsonb WHERE location_id=?",
            request.wholeOrderPrinterId(),true,write(request.itemRoutes()),write(request.categoryRoutes()),location);
        return configuration(location);
    }
    public Pairing pair(UUID location) {
        lock(location);
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("UPDATE printer_settings SET agent_token_hash=?,agent_instance=NULL,last_seen=NULL,agent_status='{}'::jsonb WHERE location_id=?",hash(token),location);
        // Rotation must never replay jobs that the previous machine might have printed.
        db.update("UPDATE print_jobs SET state='UNCERTAIN',updated_at=NOW() WHERE location_id=? AND state='CLAIMED'",location);
        return new Pairing(token);
    }
    private UUID authenticate(String token, UUID instance) {
        if(token==null || token.length()!=43) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var locations=db.queryForList("SELECT location_id FROM printer_settings WHERE agent_token_hash=? FOR UPDATE",UUID.class,hash(token));
        if(locations.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        UUID location=locations.getFirst();
        UUID owner=db.queryForObject("SELECT agent_instance FROM printer_settings WHERE location_id=?",UUID.class,location);
        if(owner!=null && !owner.equals(instance)) throw new ResponseStatusException(HttpStatus.CONFLICT,"PRINT_AGENT_ALREADY_PAIRED");
        db.update("UPDATE printer_settings SET agent_instance=? WHERE location_id=?",instance,location);
        return location;
    }
    public PollResponse poll(String token, Poll request) {
        UUID location=authenticate(token,request.instanceId());
        if(request.printerStatus().values().stream().anyMatch(v->v==null || v.length()>100)) bad("PRINT_INVALID_STATUS");
        db.update("UPDATE printer_settings SET last_seen=NOW(),agent_status=?::jsonb WHERE location_id=?",write(request.printerStatus()),location);
        // Hold overdue or no-longer-actionable orders before claiming them.
        db.update("""
            UPDATE print_jobs j SET state='HELD',updated_at=NOW() FROM orders o,printer_settings s,order_printers p
            WHERE j.order_id=o.id AND j.location_id=s.location_id AND j.printer_id=p.id AND j.location_id=?
            AND j.state='QUEUED' AND ((o.status NOT IN ('PAID','PREPARING','READY','PARTIALLY_REFUNDED') AND NOT (j.reprint AND o.status='COMPLETED'))
                OR j.created_at < NOW()-s.stale_minutes*INTERVAL '1 minute')
            """,location);
        // Bound in-flight work per printer so one unplugged station cannot starve the others.
        for (Printer printer:configuration(location).printers()) {
            db.update("""
                UPDATE print_jobs SET state='CLAIMED',updated_at=NOW() WHERE id IN
                (SELECT id FROM print_jobs WHERE printer_id=? AND state='QUEUED' ORDER BY created_at,CASE WHEN ticket_kind='WHOLE' THEN 0 ELSE 1 END,id
                 LIMIT GREATEST(0,10-(SELECT COUNT(*) FROM print_jobs WHERE printer_id=? AND state='CLAIMED'))
                 FOR UPDATE SKIP LOCKED)
                """,printer.id(),printer.id());
        }
        var jobs=db.query(jobSelect()+" WHERE j.location_id=? AND j.state='CLAIMED' ORDER BY j.created_at,CASE WHEN j.ticket_kind='WHOLE' THEN 0 ELSE 1 END,j.id LIMIT 200",jobMapper(),location);
        return new PollResponse(configuration(location),jobs);
    }
    public void acknowledge(String token, UUID id, Ack request) {
        UUID location=authenticate(token,request.instanceId());
        if(!Set.of("SENT","UNCERTAIN","HELD").contains(request.state())) bad("PRINT_INVALID_STATUS");
        int count=db.update("UPDATE print_jobs SET state=?,updated_at=NOW() WHERE id=? AND location_id=? AND state IN ('CLAIMED',?)",
            request.state(),id,location,request.state());
        if(count==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"PRINT_JOB_STATE_CHANGED");
    }
    public List<Job> jobs(UUID location) {
        return db.query(jobSelect()+" WHERE j.location_id=? ORDER BY j.created_at DESC LIMIT 100",jobMapper(),location);
    }
    public void reprint(UUID location, UUID job) {
        var orders=db.queryForList("SELECT order_id FROM print_jobs WHERE id=? AND location_id=?",UUID.class,job,location);
        if(orders.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        reprintOrder(location,orders.getFirst());
    }
    public void reprintOrder(UUID location, UUID order) {
        lock(location);
        List<Map<String,Object>> tickets=storedTickets(location,order);
        if(tickets.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"PRINT_NO_TICKETS");
        String state=db.queryForObject("SELECT status FROM orders WHERE id=?",String.class,order);
        if(!Set.of("PAID","PREPARING","READY","PARTIALLY_REFUNDED","COMPLETED").contains(state)) bad("PRINT_REPRINT_BLOCKED");
        if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM print_jobs WHERE order_id=? AND reprint AND state IN ('QUEUED','CLAIMED'))",Boolean.class,order)))
            bad("PRINT_REPRINT_BLOCKED");
        for(var ticket:tickets) insertTicket(order,location,ticket,true);
    }
    public void agentReprint(String token, UUID job, Reprint request) {
        reprint(authenticate(token, request.instanceId()), job);
    }
    // Called inside the payment transaction; uniqueness protects concurrent/repeated payment notifications.
    public void enqueue(OrderResponse order) {
        lock(order.locationId());
        // Pre-routing orders already have durable receipts but no ticket snapshot.
        // A repeated payment notification must not regenerate them using today's routes.
        if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM print_jobs WHERE order_id=? AND NOT reprint)",Boolean.class,order.id()))) return;
        var receipt=new LinkedHashMap<String,Object>();
        receipt.put("orderNumber",order.orderNumber()); receipt.put("orderType",order.orderType());
        receipt.put("customerName",order.customerName());
        receipt.put("pickupTime",order.requestedPickupTime()==null?null:order.requestedPickupTime().toString());
        if(order.userId()!=null) {
            var phones=db.queryForList("SELECT phone FROM users WHERE id=?",String.class,order.userId());
            if(!phones.isEmpty()) receipt.put("customerPhone",phones.getFirst());
        }
        receipt.put("placedAt",order.createdAt()==null?null:order.createdAt().toString());
        receipt.put("customerNote",order.customerNote()); receipt.put("items",order.items());
        receipt.put("subtotal",order.subtotal()); receipt.put("discount",order.totalDiscount());
        receipt.put("tax",order.taxAmount()); receipt.put("tip",order.tipAmount());
        receipt.put("finalTotal",order.finalTotal());
        Configuration config=configuration(order.locationId());
        List<Map<String,Object>> tickets=new ArrayList<>();
        if(!config.routingManaged()) {
            for(Printer p:config.printers()) if(p.enabled())
                tickets.add(Map.of("printerId",p.id(),"receipt",receipt,"kind","WHOLE","automatic",true));
        } else {
            UUID whole=config.wholeOrderPrinterId();
            Map<UUID,UUID> routes=new HashMap<>();
            config.itemRoutes().forEach(route->routes.put(route.menuItemId(),route.printerId()));
            Map<UUID,UUID> categoryRoutes=new HashMap<>();
            config.categoryRoutes().forEach(route->categoryRoutes.put(route.categoryId(),route.printerId()));
            Map<UUID,UUID> itemCategories=new HashMap<>();
            if(!categoryRoutes.isEmpty() && !order.items().isEmpty()) {
                UUID[] itemIds=order.items().stream().map(OrderResponse.OrderItemResponse::menuItemId).filter(Objects::nonNull).distinct().toArray(UUID[]::new);
                if(itemIds.length>0) db.query("SELECT id,category_id FROM menu_items WHERE id = ANY (?)",rs->{
                    itemCategories.put(rs.getObject("id",UUID.class),rs.getObject("category_id",UUID.class));
                },(Object)itemIds);
            }
            Set<UUID> enabled=new HashSet<>();
            config.printers().stream().filter(Printer::enabled).forEach(p->enabled.add(p.id()));
            Map<UUID,List<OrderResponse.OrderItemResponse>> groups=new LinkedHashMap<>();
            for(var item:order.items()) {
                UUID target=routes.get(item.menuItemId());
                if(target==null) target=categoryRoutes.get(itemCategories.get(item.menuItemId()));
                if(target==null) continue; // Unassigned items appear only on the complete order.
                if(!enabled.contains(target)) target=whole;
                groups.computeIfAbsent(target,k->new ArrayList<>()).add(item);
            }
            var complete=new LinkedHashMap<>(receipt);
            complete.put("wholeOrder",true);
            complete.put("fallbackPrinterId",whole);
            tickets.add(Map.of("printerId",whole,"receipt",complete,"kind","WHOLE","automatic",true));
            for(var group:groups.entrySet()) {
                var part=new LinkedHashMap<>(receipt);
                part.put("items",group.getValue());
                part.put("stationTicket",true);
                for(String key:List.of("customerPhone","placedAt","subtotal","discount","tax","tip")) part.remove(key);
                part.put("fallbackPrinterId",whole);
                tickets.add(Map.of("printerId",group.getKey(),"receipt",part,"kind","STATION","automatic",true));
            }
        }
        int inserted=db.update("INSERT INTO print_order_tickets(order_id,location_id,tickets) VALUES (?,?,?::jsonb) ON CONFLICT DO NOTHING",
            order.id(),order.locationId(),write(tickets));
        if(inserted==0) return;
        for(var ticket:tickets) if(Boolean.TRUE.equals(ticket.get("automatic"))) insertTicket(order.id(),order.locationId(),ticket,false);
    }
    private void insertTicket(UUID order,UUID location,Map<String,Object> ticket,boolean reprint) {
        String kind=(String)ticket.get("kind");
        if(kind==null) kind=((Map<?,?>)ticket.get("receipt")).containsKey("finalTotal")?"WHOLE":"STATION";
        db.update("INSERT INTO print_jobs(id,order_id,location_id,printer_id,receipt,reprint,ticket_kind) VALUES (?,?,?,?,?::jsonb,?,?)",
            UUID.randomUUID(),order,location,UUID.fromString(ticket.get("printerId").toString()),write(ticket.get("receipt")),reprint,kind);
    }
    private List<Map<String,Object>> storedTickets(UUID location,UUID order) {
        var snapshots=db.queryForList("SELECT tickets::text FROM print_order_tickets WHERE order_id=? AND location_id=?",String.class,order,location);
        if(!snapshots.isEmpty()) {
            try { return json.readValue(snapshots.getFirst(),new TypeReference<List<Map<String,Object>>>(){}); }
            catch(Exception e) { throw new IllegalStateException("Invalid print tickets",e); }
        }
        // Existing receipts remain reprintable after upgrading.
        return db.query("SELECT printer_id,receipt FROM print_jobs WHERE order_id=? AND location_id=? AND NOT reprint ORDER BY created_at",
            (rs,n)->Map.<String,Object>of("printerId",rs.getObject("printer_id",UUID.class),"receipt",parse(rs.getString("receipt"))),order,location);
    }
    private String jobSelect() { return "SELECT j.*,p.name AS printer_name,o.status AS order_status,p.enabled FROM print_jobs j JOIN order_printers p ON p.id=j.printer_id JOIN orders o ON o.id=j.order_id"; }
    private RowMapper<Job> jobMapper() { return (r,n)-> {
        var receipt=parse(r.getString("receipt"));
        receipt.put("currentOrderStatus",r.getString("order_status")); receipt.put("printerEnabled",r.getBoolean("enabled"));
        return new Job(r.getObject("id",UUID.class),r.getObject("order_id",UUID.class),r.getObject("printer_id",UUID.class),
            r.getString("printer_name"),r.getString("state"),r.getBoolean("reprint"),r.getTimestamp("created_at").toInstant(),receipt);
    }; }
    private Map<String,Object> parse(String s) { try { return json.readValue(s,new TypeReference<LinkedHashMap<String,Object>>(){}); } catch(Exception e) { throw new IllegalStateException("Invalid print JSON",e); } }
    private String write(Object value) { try { return json.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException("Cannot serialize print job",e); } }
    private static String hash(String token) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    private static void bad(String code) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,code); }
}
