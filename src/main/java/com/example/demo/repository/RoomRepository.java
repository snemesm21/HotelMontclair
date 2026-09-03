package com.example.demo.repository;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Repository
public class RoomRepository {
    private final Map<Long, Room> store = new LinkedHashMap<>();
    private long idGen = 1L;

    @PostConstruct
    public void init() {

        // 1. Deluxe Room
        Room r1 = new Room(null, "101", 1, 1L, RoomStatus.AVAILABLE);
        r1.setName("Deluxe Room");
        r1.setCapacity(2);
        r1.setBedType("King Size");
        r1.setArea(30);
        r1.setPricePerNight(116.52);
        r1.setImageUrl("https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80");
        r1.setDescription("Elegancia y confort en un espacio acogedor con vistas inolvidables.");
        r1.setHeroDescription("Una habitación diseñada para quienes buscan el equilibrio perfecto entre lujo discreto y confort absoluto. Materiales nobles, iluminación cálida y vistas que invitan al descanso.");
        r1.setHeadline("El refugio perfecto para un descanso revitalizante");
        r1.setFullDescription("Nuestra Deluxe Room combina un diseño contemporáneo con toques clásicos europeos. Suelos de roble, ropa de cama de algodón egipcio de 400 hilos, minibar premium y un baño revestido en mármol de Carrara con ducha de efecto lluvia.");
        r1.setSecondaryImageUrl("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        r1.addHighlight("Cama King Size con colchón premium", "Colchón de muelles ensacados con topper de pluma de ganso para un descanso reparador.");
        r1.addHighlight("Baño en mármol con ducha efecto lluvia", "Amenities de cortesía Hermès y albornoz de algodón turco.");
        r1.addHighlight("Smart TV 55\" y WiFi de alta velocidad", "Acceso a plataformas de streaming y conectividad empresarial.");
        r1.addHighlight("Servicio de habitaciones 24 horas", "Carta gourmet disponible a cualquier hora con presentación impecable.");
        r1.addGalleryImage("https://images.unsplash.com/photo-1590490360182-c33d955c4644?auto=format&fit=crop&w=800&q=80");
        r1.addGalleryImage("https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=800&q=80");
        r1.addGalleryImage("https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        save(r1);

        // 2. Premium Suite
        Room r2 = new Room(null, "202", 2, 2L, RoomStatus.AVAILABLE);
        r2.setName("Premium Suite");
        r2.setCapacity(2);
        r2.setBedType("King Size");
        r2.setArea(40);
        r2.setPricePerNight(172.01);
        r2.setImageUrl("https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
        r2.setDescription("Amplias suites con sala privada y balcón con vista panorámica.");
        r2.setHeroDescription("Un espacio generoso donde la elegancia se funde con la funcionalidad. Sala de estar independiente, balcón privado y vistas panorámicas al Mediterráneo.");
        r2.setHeadline("Amplitud y sofisticación con vistas al horizonte");
        r2.setFullDescription("La Premium Suite ofrece una experiencia elevada con sala de estar separada, escritorio ejecutivo, vestidor walk-in y un balcón privado con mobiliario de exterior premium. Ideal para estancias prolongadas o viajeros que valoran el espacio.");
        r2.setSecondaryImageUrl("https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=800&q=80");
        r2.addHighlight("Sala de estar independiente", "Sofá de diseño italiano, mesa de centro y biblioteca curada.");
        r2.addHighlight("Balcón privado con vistas al mar", "Mobiliario de exterior y servicio de desayuno al aire libre.");
        r2.addHighlight("Vestidor walk-in iluminado", "Espacio amplio con perchas de cedro y caja fuerte de gran formato.");
        r2.addHighlight("Máquina Nespresso y minibar premium", "Selección de cápsulas de origen único y bebidas artesanales.");
        r2.addGalleryImage("https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80");
        r2.addGalleryImage("https://images.unsplash.com/photo-1560448204-603b3fc33ddc?auto=format&fit=crop&w=800&q=80");
        r2.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        save(r2);

        // 3. Grand Suite
        Room r3 = new Room(null, "301", 3, 2L, RoomStatus.OCCUPIED);
        r3.setName("Grand Suite");
        r3.setCapacity(4);
        r3.setBedType("King Size");
        r3.setArea(80);
        r3.setPricePerNight(255.34);
        r3.setImageUrl("https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80");
        r3.setDescription("Lujo sofisticado con espacios exclusivos y servicios premium.");
        r3.setHeroDescription("La máxima expresión del lujo residencial. Espacios amplios con decoración de autor, comedor privado y una terraza que domina la costa del Principado.");
        r3.setHeadline("Donde el lujo se convierte en un estilo de vida");
        r3.setFullDescription("La Grand Suite redefine la hospitalidad con más de 80 m² de espacio habitable. Dormitorio principal con vestidor, segundo dormitorio opcional, comedor para cuatro personas, baño doble con bañera exenta y terraza panorámica privada.");
        r3.setSecondaryImageUrl("https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80");
        r3.addHighlight("Dos dormitorios con opción de conectar", "Configuración flexible para familias o comitivas ejecutivas.");
        r3.addHighlight("Comedor privado para 4 personas", "Mesa de mármol con servicio de chef privado bajo petición.");
        r3.addHighlight("Bañera exenta y doble lavabo", "Bañera de piedra natural con sales aromáticas de cortesía.");
        r3.addHighlight("Terraza panorámica de 20 m²", "Tumbonas, sombrilla y servicio de cócteles al atardecer.");
        r3.addGalleryImage("https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=800&q=80");
        r3.addGalleryImage("https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=800&q=80");
        r3.addGalleryImage("https://images.unsplash.com/photo-1564078516393-cf04bd966897?auto=format&fit=crop&w=800&q=80");
        save(r3);

        // 4. Suite Royale
        Room r4 = new Room(null, "401", 4, 2L, RoomStatus.AVAILABLE);
        r4.setName("Suite Royale");
        r4.setCapacity(2);
        r4.setBedType("King Size");
        r4.setArea(60);
        r4.setPricePerNight(350.00);
        r4.setImageUrl("https://images.unsplash.com/photo-1590490360182-c33d955c4644?auto=format&fit=crop&w=800&q=80");
        r4.setDescription("Nuestra suite más independiente, exclusiva y personalizada.");
        r4.setHeroDescription("Reservada para los huéspedes más distinguidos. Un santuario privado con mayordomo dedicado, acceso directo al spa y los más altos estándares de discreción.");
        r4.setHeadline("La experiencia definitiva en hospitalidad de autor");
        r4.setFullDescription("La Suite Royale es nuestra joya. Decorada con piezas de arte originales, chimenea de bioetanol, piano de cola Steinway y un jacuzzi privado en la terraza. Incluye servicio de mayordomo 24h y check-in privado.");
        r4.setSecondaryImageUrl("https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=800&q=80");
        r4.addHighlight("Mayordomo personal 24 horas", "Atención exclusiva para cada detalle de tu estancia.");
        r4.addHighlight("Jacuzzi privado en terraza", "Hidromasaje con vistas al Mediterráneo y servicio de champán.");
        r4.addHighlight("Obras de arte originales", "Colección curada de artistas contemporáneos del Principado.");
        r4.addHighlight("Check-in y check-out privado", "Recepción en suite con champagne de bienvenida Krug.");
        r4.addGalleryImage("https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80");
        r4.addGalleryImage("https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
        r4.addGalleryImage("https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80");
        save(r4);
    }

    public List<Room> findAll() { return new ArrayList<>(store.values()); }
    public Room findById(Long id) { return store.get(id); }
    public Room save(Room room) {
        if (room.getId() == null) room.setId(idGen++);
        store.put(room.getId(), room);
        return room;
    }
    public void delete(Long id) { store.remove(id); }
}
