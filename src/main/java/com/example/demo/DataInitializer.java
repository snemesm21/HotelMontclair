package com.example.demo;

import com.example.demo.entities.Client;
import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.RoomType;
import com.example.demo.entities.Service;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ClientRepository clientRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;

    public DataInitializer(ClientRepository clientRepository, RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository, ServiceRepository serviceRepository) {
        this.clientRepository = clientRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void run(String... args) {
        if (clientRepository.count() == 0) {
            clientRepository.save(new Client(null, "admin", "admin", "admin@hotelmontclair.com",
                    "Administrador", "Montclair", null, "+34 910 000 001", "ADMIN"));
            clientRepository.save(new Client(null, "demo", "demo", "demo@example.com",
                    "Demo", "User", null, "+34 612 345 678", "CLIENT"));
            for (int index = 3; index <= 10; index++) {
                clientRepository.save(new Client(null, "cliente" + index, "cliente" + index,
                        "cliente" + index + "@example.com", "Cliente", "Montclair", null,
                        "+34 612 345 6" + String.format("%02d", index), "CLIENT"));
            }
        }

        if (roomTypeRepository.count() == 0) {
            List<RoomType> types = new ArrayList<>();
            types.add(roomTypeRepository.save(RoomType.builder().name("Simple").description("Habitación estándar").pricePerNight(80.0).build()));
            types.add(roomTypeRepository.save(RoomType.builder().name("Deluxe").description("Habitación amplia con servicios premium").pricePerNight(116.52).build()));
            types.add(roomTypeRepository.save(RoomType.builder().name("Suite").description("Habitación de lujo con sala privada").pricePerNight(200.0).build()));
            types.add(roomTypeRepository.save(RoomType.builder().name("Premium Suite").description("Suite exclusiva con balcón privado").pricePerNight(280.0).build()));
            types.add(roomTypeRepository.save(RoomType.builder().name("Suite Royale").description("La experiencia más exclusiva del hotel").pricePerNight(350.0).build()));

            for (int index = 1; index <= 50; index++) {
                RoomType type = types.get((index - 1) % types.size());
                Room room = new Room(null, String.valueOf(100 + index), ((index - 1) / 10) + 1,
                        type.getId(), index % 7 == 0 ? RoomStatus.OCCUPIED : RoomStatus.AVAILABLE);
                room.setType(type);
                room.setName(type.getName());
                room.setCapacity(type.getName().contains("Suite") ? 4 : 2);
                room.setBedType("King Size");
                room.setArea(type.getName().contains("Suite") ? 60 : 30);
                room.setPricePerNight(type.getPricePerNight());
                room.setImageUrl(roomImage(index));
                room.setDescription(type.getDescription());
                applyOriginalRoomContent(room, index);
                roomRepository.save(room);
            }
        }

        if (serviceRepository.count() == 0) {
            saveRestaurant();
            saveSpa();
            saveGym();
            savePool();
            saveEvents();
            saveRoomService();
            saveLaundry();
            saveTransfers();
        }
    }

    private String roomImage(int index) {
        String[] images = {
            "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1590490360182-c33d955c4644?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1560448204-603b3fc33ddc?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=800&q=80"
        };
        return images[(index - 1) % images.length];
    }

    private void applyOriginalRoomContent(Room room, int index) {
        if (index == 1) {
            room.setName("Deluxe Room");
            room.setHeroDescription("Una habitación diseñada para quienes buscan el equilibrio perfecto entre lujo discreto y confort absoluto. Materiales nobles, iluminación cálida y vistas que invitan al descanso.");
            room.setHeadline("El refugio perfecto para un descanso revitalizante");
            room.setFullDescription("Nuestra Deluxe Room combina un diseño contemporáneo con toques clásicos europeos. Suelos de roble, ropa de cama de algodón egipcio de 400 hilos, minibar premium y un baño revestido en mármol de Carrara con ducha de efecto lluvia.");
            room.setSecondaryImageUrl("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
            room.addHighlight("Cama King Size con colchón premium", "Colchón de muelles ensacados con topper de pluma de ganso para un descanso reparador.");
            room.addHighlight("Baño en mármol con ducha efecto lluvia", "Amenities de cortesía Hermès y albornoz de algodón turco.");
            room.addHighlight("Smart TV 55\" y WiFi de alta velocidad", "Acceso a plataformas de streaming y conectividad empresarial.");
            room.addHighlight("Servicio de habitaciones 24 horas", "Carta gourmet disponible a cualquier hora con presentación impecable.");
            room.addGalleryImage("https://images.unsplash.com/photo-1590490360182-c33d955c4644?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        } else if (index == 2) {
            room.setName("Premium Suite");
            room.setHeroDescription("Un espacio generoso donde la elegancia se funde con la funcionalidad. Sala de estar independiente, balcón privado y vistas panorámicas al Mediterráneo.");
            room.setHeadline("Amplitud y sofisticación con vistas al horizonte");
            room.setFullDescription("La Premium Suite ofrece una experiencia elevada con sala de estar separada, escritorio ejecutivo, vestidor walk-in y un balcón privado con mobiliario de exterior premium. Ideal para estancias prolongadas o viajeros que valoran el espacio.");
            room.setSecondaryImageUrl("https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=800&q=80");
            room.addHighlight("Sala de estar independiente", "Sofá de diseño italiano, mesa de centro y biblioteca curada.");
            room.addHighlight("Balcón privado con vistas al mar", "Mobiliario de exterior y servicio de desayuno al aire libre.");
            room.addHighlight("Vestidor walk-in iluminado", "Espacio amplio con perchas de cedro y caja fuerte de gran formato.");
            room.addHighlight("Máquina Nespresso y minibar premium", "Selección de cápsulas de origen único y bebidas artesanales.");
            room.addGalleryImage("https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1560448204-603b3fc33ddc?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        } else if (index == 3) {
            room.setName("Grand Suite");
            room.setHeroDescription("La máxima expresión del lujo residencial. Espacios amplios con decoración de autor, comedor privado y una terraza que domina la costa del Principado.");
            room.setHeadline("Donde el lujo se convierte en un estilo de vida");
            room.setFullDescription("La Grand Suite redefine la hospitalidad con más de 80 m² de espacio habitable. Dormitorio principal con vestidor, segundo dormitorio opcional, comedor para cuatro personas, baño doble con bañera exenta y terraza panorámica privada.");
            room.setSecondaryImageUrl("https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80");
            room.addHighlight("Dos dormitorios con opción de conectar", "Configuración flexible para familias o comitivas ejecutivas.");
            room.addHighlight("Comedor privado para 4 personas", "Mesa de mármol con servicio de chef privado bajo petición.");
            room.addHighlight("Bañera exenta y doble lavabo", "Bañera de piedra natural con sales aromáticas de cortesía.");
            room.addHighlight("Terraza panorámica de 20 m²", "Tumbonas, sombrilla y servicio de cócteles al atardecer.");
            room.addGalleryImage("https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1564078516393-cf04bd966897?auto=format&fit=crop&w=800&q=80");
        } else if (index == 4) {
            room.setName("Suite Royale");
            room.setHeroDescription("Reservada para los huéspedes más distinguidos. Un santuario privado con mayordomo dedicado, acceso directo al spa y los más altos estándares de discreción.");
            room.setHeadline("La experiencia definitiva en hospitalidad de autor");
            room.setFullDescription("La Suite Royale es nuestra joya. Decorada con piezas de arte originales, chimenea de bioetanol, piano de cola Steinway y un jacuzzi privado en la terraza. Incluye servicio de mayordomo 24h y check-in privado.");
            room.setSecondaryImageUrl("https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=800&q=80");
            room.addHighlight("Mayordomo personal 24 horas", "Atención exclusiva para cada detalle de tu estancia.");
            room.addHighlight("Jacuzzi privado en terraza", "Hidromasaje con vistas al Mediterráneo y servicio de champán.");
            room.addHighlight("Obras de arte originales", "Colección curada de artistas contemporáneos del Principado.");
            room.addHighlight("Check-in y check-out privado", "Recepción en suite con champagne de bienvenida Krug.");
            room.addGalleryImage("https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
            room.addGalleryImage("https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80");
        }
    }

    private void saveRestaurant() {
        Service service = new Service(null, "EXPERIENCIA 01", "Restaurante",
                "Gastronomía de autor con ingredientes frescos y locales, presentada en un ambiente elegante y sofisticado.",
                "6:30 AM - 11:30 PM", 40.0, "Desde € 40 EUR",
                "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Gastronomía de autor concebida para los paladares más exigentes. Cocina mediterránea refinada, producto local y una cava histórica frente al mar de Mónaco.");
        service.setTagline("HAUTE CUISINE & MARIDAJE");
        service.setHeadline("Un viaje gastronómico donde el sabor y el arte se encuentran");
        service.setFullDescription("Déjate seducir por un menú diseñado por nuestro chef ejecutivo, combinando la frescura del Mediterráneo con técnicas de alta cocina internacional en una atmósfera de luz tenue y servicio impecable.");
        service.setScheduleNote("Desayuno, Almuerzo y Cena");
        service.setPriceNote("A la carta o menú maridaje");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Menú degustación de 7 tiempos", "Creaciones de autor con maridaje seleccionado por sumiller.");
        service.addHighlight("Cava privada de añadas históricas", "Etiquetas exclusivas de Burdeos, Champagne y la Provenza.");
        service.addHighlight("Chef's Table confidencial", "Mesa privada frente a la cocina para un máximo de 6 comensales.");
        service.addHighlight("Terraza panorámica con vistas al puerto", "Cenas bajo el cielo estrellado del Principado.");
        service.addGalleryImage("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveSpa() {
        Service service = new Service(null, "EXPERIENCIA 02", "Spa & Bienestar",
                "Tratamientos relajantes, masajes y circuitos de bienestar diseñados para una pausa íntima y exclusiva.",
                "8:00 AM - 10:00 PM", 100.0, "Desde €100 EUR",
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Un santuario privado concebido para quienes buscan silencio, exclusividad y bienestar absoluto. Rituales de autor, espacios de mármol y experiencias sensoriales diseñadas hasta el último detalle.");
        service.setTagline("SIGNATURE SPA EXPERIENCE");
        service.setHeadline("Un oasis de tranquilidad para cuerpo y mente");
        service.setFullDescription("Sumérgete en una experiencia de bienestar reservada para quienes esperan algo excepcional. Nuestro spa combina arquitectura íntima, tratamientos de autor y rituales personalizados en un entorno diseñado para desconectarte del mundo exterior.");
        service.setScheduleNote("Todos los días");
        service.setPriceNote("Por persona · reserva previa");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Masajes signature y terapéuticos", "Técnicas personalizadas y aceites premium seleccionados para ti.");
        service.addHighlight("Circuito privado de hidroterapia", "Piscina climatizada, contrastes térmicos y zonas de inmersión.");
        service.addHighlight("Rituales faciales y corporales", "Protocolos de alta gama enfocados en restauración profunda.");
        service.addHighlight("Sauna, vapor y lounge de relajación", "Ambientes de acceso limitado para una experiencia más íntima.");
        service.addGalleryImage("https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveGym() {
        Service service = new Service(null, "EXPERIENCIA 03", "Gimnasio",
                "Equipos de última generación y espacios preparados para entrenamiento personal durante tu estancia.",
                "6:00 AM - 10:00 PM", 0.0, "Incluido",
                "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Instalaciones de alto rendimiento diseñadas para mantener tu vitalidad y bienestar físico con la más avanzada tecnología y atención personalizada.");
        service.setTagline("WELLNESS & HIGH PERFORMANCE");
        service.setHeadline("Entrenamiento de élite y energía renovada en cada sesión");
        service.setFullDescription("Espacios luminosos y diáfanos equipados con la gama más avanzada de Technogym, zonas de peso libre, cardio inmersivo y asesoramiento deportivo a medida.");
        service.setScheduleNote("Acceso 24h para huéspedes");
        service.setPriceNote("Acceso ilimitado durante la estancia");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Equipamiento Technogym Artis", "Maquinaria biomecánica de precisión con conectividad digital integrada.");
        service.addHighlight("Entrenamiento personal bajo demanda", "Entrenadores certificados para sesiones a medida de fuerza y movilidad.");
        service.addHighlight("Estudio de Yoga, Pilates & Mindfulness", "Clases guiadas matutinas para despertar cuerpo y mente.");
        service.addHighlight("Área de recuperación y toallas frías", "Hidratación con aguas infusionadas y servicio de toallas de algodón egipcio.");
        service.addGalleryImage("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1571902943202-507ec2618e8f?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void savePool() {
        Service service = new Service(null, "EXPERIENCIA 04", "Piscina",
                "Piscina climatizada con vista panorámica y zona de descanso para disfrutar del entorno mediterráneo.",
                "7:00 AM - 9:00 PM", 0.0, "Incluido",
                "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Un espejo de agua infinita suspendido sobre la costa de Mónaco. Climatización perfecta, solárium privado y servicio de coctelería junto al agua.");
        service.setTagline("PANORAMIC INFINITY POOL");
        service.setHeadline("El placer de nadar con el Mediterráneo en el horizonte");
        service.setFullDescription("Relájate en nuestras tumbonas premium mientras disfrutas de una temperatura de agua constante a 28°C, toallas aromáticas y atención personalizada de nuestros camareros de piscina.");
        service.setScheduleNote("Todos los días");
        service.setPriceNote("Acceso exclusivo para huéspedes");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Agua climatizada todo el año", "Sistema de filtración salina respetuoso con la piel a 28°C.");
        service.addHighlight("Solárium privado con camas balinesas", "Espacios de sombraje natural y vistas directas a la bahía.");
        service.addHighlight("Pool Bar & Snacks saludables", "Smoothies revitalizantes, frutas de temporada y cócteles de autor.");
        service.addHighlight("Servicio de toallas y amenidades", "Protectores solares orgánicos y brumas refrescantes.");
        service.addGalleryImage("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveEvents() {
        Service service = new Service(null, "EXPERIENCIA 05", "Salones de eventos",
                "Espacios elegantes para eventos corporativos, recepciones privadas y celebraciones especiales.",
                "8:00 AM - 12:00 AM", 0.0, "A consultar",
                "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Escenarios sofisticados donde cada detalle técnico y gastronómico se orquesta con máxima precisión para recepciones, juntas y celebraciones memorables.");
        service.setTagline("EXCLUSIVE GATHERINGS & GALAS");
        service.setHeadline("El marco perfecto para tus momentos más distinguidos");
        service.setFullDescription("Salones modulables de gran altura revestidos en maderas nobles y mármol, equipados con acústica de concierto, proyecciones láser 4K y servicio de catering de alta cocina.");
        service.setScheduleNote("Reserva según disponibilidad");
        service.setPriceNote("Presupuestos a medida");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1464366400600-7168b8af9bc3?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Salones modulables hasta 300 invitados", "Configuraciones versátiles para banquetes, cócteles o conferencias.");
        service.addHighlight("Tecnología audiovisual inmersiva", "Sistemas de sonido envolvente, pantallas 4K y streaming privado.");
        service.addHighlight("Banquetería y sumillería exclusiva", "Menús personalizados creados por nuestros chefs de banquete.");
        service.addHighlight("Planificador de eventos dedicado", "Asistencia personalizada desde la concepción hasta el cierre del evento.");
        service.addGalleryImage("https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1527529482837-4698179dc6ce?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveRoomService() {
        Service service = new Service(null, "EXPERIENCIA 06", "Room Service",
                "Servicio a la habitación disponible las 24 horas, con una presentación cuidada y atención personalizada.",
                "24 horas", 0.0, "Sin cargo adicional",
                "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("La excelencia culinaria de Montclair servida en la intimidad de tu suite. Desayunos gourmet en el balcón, cenas a la luz de las velas y refrigerios nocturnos.");
        service.setTagline("IN-SUITE DINING 24/7");
        service.setHeadline("Gastronomía de primer nivel en la privacidad de tu suite");
        service.setFullDescription("Disfruta de platos recién preparados, carritos térmicos de plata y un servicio discreto y puntual a cualquier hora del día o de la noche.");
        service.setScheduleNote("Disponible 24/7");
        service.setPriceNote("Carta de precios según consumición");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Desayunos de autor servidos al amanecer", "Huevos benedictinos, bollería recién horneada y zumos naturales.");
        service.addHighlight("Carta nocturna de platos calientes", "Opciones gastronómicas reconfortantes disponibles toda la noche.");
        service.addHighlight("Servicio de champán y caviar", "Presentación en hielo con copas de cristal de Baccarat.");
        service.addHighlight("Atención personalizada y discreta", "Montaje de mesa completo en la terraza o salón de la suite.");
        service.addGalleryImage("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1482049016688-2d3e1b311543?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveLaundry() {
        Service service = new Service(null, "EXPERIENCIA 07", "Servicio de lavandería",
                "Lavandería y tintorería exprés con una presentación sobria, cuidada y acorde con la experiencia premium del hotel.",
                "7:00 AM - 7:00 PM", 30.0, "Desde € 30 EUR",
                "https://images.unsplash.com/photo-1582735689369-4fe89db7114c?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Cuidado minucioso para tus prendas más delicadas. Técnicas ecológicas de planchado y tintorería artesanal entregadas con funda protectora en tu armario.");
        service.setTagline("EXPRESS VALET & DRY CLEANING");
        service.setHeadline("El cuidado más exigente para tus mejores prendas");
        service.setFullDescription("Tratamiento textil de alta gama para seda, lino, lana virgen y trajes a medida, con servicio exprés en el mismo día y plegado en papel de seda.");
        service.setScheduleNote("Lunes a Domingo");
        service.setPriceNote("Por prenda o servicio completo");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1545173168-9f1947eebb7f?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Servicio exprés en 4 horas", "Recogida y entrega directa en tu habitación.");
        service.addHighlight("Tratamientos eco-friendly", "Productos hipoalergénicos y técnicas sin químicos agresivos.");
        service.addHighlight("Planchado artesanal al vapor", "Cuidado especializado para trajes de noche y camisas de etiqueta.");
        service.addHighlight("Presentación impecable", "Perchas de madera forrada y fundas protectoras transpirables.");
        service.addGalleryImage("https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1604335399105-a0c585fd81a1?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1582735689369-4fe89db7114c?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }

    private void saveTransfers() {
        Service service = new Service(null, "EXPERIENCIA 08", "Traslados",
                "Servicio privado con vehículos premium y chauffeur para desplazamientos por Mónaco y la Costa Azul.",
                "24 horas", 0.0, "A consultar",
                "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80");
        service.setHeroDescription("Flota exclusiva de berlinas y minivans de alta gama con chóferes multilingües para traslados al Aeropuerto de Niza, helipuerto y destinos exclusivos de la Riviera.");
        service.setTagline("CHAUFFEUR & LUXURY FLEET");
        service.setHeadline("Movilidad de lujo con puntualidad y distinción absoluta");
        service.setFullDescription("Viaja con la máxima serenidad y confort en vehículos equipados con Wi-Fi de alta velocidad, agua mineral artesanal y atención a cada uno de tus itinerarios.");
        service.setScheduleNote("Reserva 24/7 previa");
        service.setPriceNote("Tarifas fijas por trayecto");
        service.setSecondaryImageUrl("https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&w=800&q=80");
        service.addHighlight("Flota Mercedes-Benz Clase S y Maybach", "Vehículos insonorizados con tapicería de cuero nappa.");
        service.addHighlight("Chóferes profesionales y bilingües", "Conocimiento experto de las rutas y protocolos de la Riviera.");
        service.addHighlight("Conexión directa Aeropuerto Niza-Costa Azul", "Recepción personalizada en terminal con cartel y asistencia de equipaje.");
        service.addHighlight("Servicio a disposición por horas", "Flexibilidad absoluta para compras, reuniones o paseos nocturnos.");
        service.addGalleryImage("https://images.unsplash.com/photo-1502877338535-766e1452684a?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80");
        service.addGalleryImage("https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80");
        serviceRepository.save(service);
    }
}