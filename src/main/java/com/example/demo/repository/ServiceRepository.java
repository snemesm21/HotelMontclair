package com.example.demo.repository;

import com.example.demo.entities.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ServiceRepository {
    private final Map<Long, Service> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 1. Restaurante
        Service s1 = new Service(
            1L,
            "EXPERIENCIA 01",
            "Restaurante",
            "Gastronomía de autor con ingredientes frescos y locales, presentada en un ambiente elegante y sofisticado.",
            "6:30 AM - 11:30 PM",
            40.0,
            "Desde € 40 EUR",
            "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=800&q=80"
        );
        s1.setHeroDescription("Gastronomía de autor concebida para los paladares más exigentes. Cocina mediterránea refinada, producto local y una cava histórica frente al mar de Mónaco.");
        s1.setTagline("HAUTE CUISINE & MARIDAJE");
        s1.setHeadline("Un viaje gastronómico donde el sabor y el arte se encuentran");
        s1.setFullDescription("Déjate seducir por un menú diseñado por nuestro chef ejecutivo, combinando la frescura del Mediterráneo con técnicas de alta cocina internacional en una atmósfera de luz tenue y servicio impecable.");
        s1.setScheduleNote("Desayuno, Almuerzo y Cena");
        s1.setPriceNote("A la carta o menú maridaje");
        s1.setSecondaryImageUrl("https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=800&q=80");
        s1.addHighlight("Menú degustación de 7 tiempos", "Creaciones de autor con maridaje seleccionado por sumiller.");
        s1.addHighlight("Cava privada de añadas históricas", "Etiquetas exclusivas de Burdeos, Champagne y la Provenza.");
        s1.addHighlight("Chef's Table confidencial", "Mesa privada frente a la cocina para un máximo de 6 comensales.");
        s1.addHighlight("Terraza panorámica con vistas al puerto", "Cenas bajo el cielo estrellado del Principado.");
        s1.addGalleryImage("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80");
        s1.addGalleryImage("https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=800&q=80");
        s1.addGalleryImage("https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80");
        save(s1);

        // 2. Spa & Bienestar (Matching Screenshot 100%)
        Service s2 = new Service(
            2L,
            "EXPERIENCIA 02",
            "Spa & Bienestar",
            "Tratamientos relajantes, masajes y circuitos de bienestar diseñados para una pausa íntima y exclusiva.",
            "8:00 AM - 10:00 PM",
            100.0,
            "Desde €100 EUR",
            "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80"
        );
        s2.setHeroDescription("Un santuario privado concebido para quienes buscan silencio, exclusividad y bienestar absoluto. Rituales de autor, espacios de mármol y experiencias sensoriales diseñadas hasta el último detalle.");
        s2.setTagline("SIGNATURE SPA EXPERIENCE");
        s2.setHeadline("Un oasis de tranquilidad para cuerpo y mente");
        s2.setFullDescription("Sumérgete en una experiencia de bienestar reservada para quienes esperan algo excepcional. Nuestro spa combina arquitectura íntima, tratamientos de autor y rituales personalizados en un entorno diseñado para desconectarte del mundo exterior.");
        s2.setScheduleNote("Todos los días");
        s2.setPriceNote("Por persona · reserva previa");
        s2.setSecondaryImageUrl("https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=800&q=80");
        s2.addHighlight("Masajes signature y terapéuticos", "Técnicas personalizadas y aceites premium seleccionados para ti.");
        s2.addHighlight("Circuito privado de hidroterapia", "Piscina climatizada, contrastes térmicos y zonas de inmersión.");
        s2.addHighlight("Rituales faciales y corporales", "Protocolos de alta gama enfocados en restauración profunda.");
        s2.addHighlight("Sauna, vapor y lounge de relajación", "Ambientes de acceso limitado para una experiencia más íntima.");
        s2.addGalleryImage("https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?auto=format&fit=crop&w=800&q=80");
        s2.addGalleryImage("https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=800&q=80");
        s2.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        save(s2);

        // 3. Gimnasio
        Service s3 = new Service(
            3L,
            "EXPERIENCIA 03",
            "Gimnasio",
            "Equipos de última generación y espacios preparados para entrenamiento personal durante tu estancia.",
            "6:00 AM - 10:00 PM",
            0.0,
            "Incluido",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80"
        );
        s3.setHeroDescription("Instalaciones de alto rendimiento diseñadas para mantener tu vitalidad y bienestar físico con la más avanzada tecnología y atención personalizada.");
        s3.setTagline("WELLNESS & HIGH PERFORMANCE");
        s3.setHeadline("Entrenamiento de élite y energía renovada en cada sesión");
        s3.setFullDescription("Espacios luminosos y diáfanos equipados con la gama más avanzada de Technogym, zonas de peso libre, cardio inmersivo y asesoramiento deportivo a medida.");
        s3.setScheduleNote("Acceso 24h para huéspedes");
        s3.setPriceNote("Acceso ilimitado durante la estancia");
        s3.setSecondaryImageUrl("https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&w=800&q=80");
        s3.addHighlight("Equipamiento Technogym Artis", "Maquinaria biomecánica de precisión con conectividad digital integrada.");
        s3.addHighlight("Entrenamiento personal bajo demanda", "Entrenadores certificados para sesiones a medida de fuerza y movilidad.");
        s3.addHighlight("Estudio de Yoga, Pilates & Mindfulness", "Clases guiadas matutinas para despertar cuerpo y mente.");
        s3.addHighlight("Área de recuperación y toallas frías", "Hidratación con aguas infusionadas y servicio de toallas de algodón egipcio.");
        s3.addGalleryImage("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80");
        s3.addGalleryImage("https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80");
        s3.addGalleryImage("https://images.unsplash.com/photo-1571902943202-507ec2618e8f?auto=format&fit=crop&w=800&q=80");
        save(s3);

        // 4. Piscina (EXPERIENCIA 04)
        Service s4 = new Service(
            4L,
            "EXPERIENCIA 04",
            "Piscina",
            "Piscina climatizada con vista panorámica y zona de descanso para disfrutar del entorno mediterráneo.",
            "7:00 AM - 9:00 PM",
            0.0,
            "Incluido",
            "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?auto=format&fit=crop&w=800&q=80"
        );
        s4.setHeroDescription("Un espejo de agua infinita suspendido sobre la costa de Mónaco. Climatización perfecta, solárium privado y servicio de coctelería junto al agua.");
        s4.setTagline("PANORAMIC INFINITY POOL");
        s4.setHeadline("El placer de nadar con el Mediterráneo en el horizonte");
        s4.setFullDescription("Relájate en nuestras tumbonas premium mientras disfrutas de una temperatura de agua constante a 28°C, toallas aromáticas y atención personalizada de nuestros camareros de piscina.");
        s4.setScheduleNote("Todos los días");
        s4.setPriceNote("Acceso exclusivo para huéspedes");
        s4.setSecondaryImageUrl("https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80");
        s4.addHighlight("Agua climatizada todo el año", "Sistema de filtración salina respetuoso con la piel a 28°C.");
        s4.addHighlight("Solárium privado con camas balinesas", "Espacios de sombraje natural y vistas directas a la bahía.");
        s4.addHighlight("Pool Bar & Snacks saludables", "Smoothies revitalizantes, frutas de temporada y cócteles de autor.");
        s4.addHighlight("Servicio de toallas y amenidades", "Protectores solares orgánicos y brumas refrescantes.");
        s4.addGalleryImage("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80");
        s4.addGalleryImage("https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80");
        s4.addGalleryImage("https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80");
        save(s4);

        // 5. Salones de eventos (EXPERIENCIA 05)
        Service s5 = new Service(
            5L,
            "EXPERIENCIA 05",
            "Salones de eventos",
            "Espacios elegantes para eventos corporativos, recepciones privadas y celebraciones especiales.",
            "8:00 AM - 12:00 AM",
            0.0,
            "A consultar",
            "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?auto=format&fit=crop&w=800&q=80"
        );
        s5.setHeroDescription("Escenarios sofisticados donde cada detalle técnico y gastronómico se orquesta con máxima precisión para recepciones, juntas y celebraciones memorables.");
        s5.setTagline("EXCLUSIVE GATHERINGS & GALAS");
        s5.setHeadline("El marco perfecto para tus momentos más distinguidos");
        s5.setFullDescription("Salones modulables de gran altura revestidos en maderas nobles y mármol, equipados con acústica de concierto, proyecciones láser 4K y servicio de catering de alta cocina.");
        s5.setScheduleNote("Reserva según disponibilidad");
        s5.setPriceNote("Presupuestos a medida");
        s5.setSecondaryImageUrl("https://images.unsplash.com/photo-1464366400600-7168b8af9bc3?auto=format&fit=crop&w=800&q=80");
        s5.addHighlight("Salones modulables hasta 300 invitados", "Configuraciones versátiles para banquetes, cócteles o conferencias.");
        s5.addHighlight("Tecnología audiovisual inmersiva", "Sistemas de sonido envolvente, pantallas 4K y streaming privado.");
        s5.addHighlight("Banquetería y sumillería exclusiva", "Menús personalizados creados por nuestros chefs de banquete.");
        s5.addHighlight("Planificador de eventos dedicado", "Asistencia personalizada desde la concepción hasta el cierre del evento.");
        s5.addGalleryImage("https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=800&q=80");
        s5.addGalleryImage("https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=800&q=80");
        s5.addGalleryImage("https://images.unsplash.com/photo-1527529482837-4698179dc6ce?auto=format&fit=crop&w=800&q=80");
        save(s5);

        // 6. Room Service (EXPERIENCIA 06)
        Service s6 = new Service(
            6L,
            "EXPERIENCIA 06",
            "Room Service",
            "Servicio a la habitación disponible las 24 horas, con una presentación cuidada y atención personalizada.",
            "24 horas",
            0.0,
            "Sin cargo adicional",
            "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?auto=format&fit=crop&w=800&q=80"
        );
        s6.setHeroDescription("La excelencia culinaria de Montclair servida en la intimidad de tu suite. Desayunos gourmet en el balcón, cenas a la luz de las velas y refrigerios nocturnos.");
        s6.setTagline("IN-SUITE DINING 24/7");
        s6.setHeadline("Gastronomía de primer nivel en la privacidad de tu suite");
        s6.setFullDescription("Disfruta de platos recién preparados, carritos térmicos de plata y un servicio discreto y puntual a cualquier hora del día o de la noche.");
        s6.setScheduleNote("Disponible 24/7");
        s6.setPriceNote("Carta de precios según consumición");
        s6.setSecondaryImageUrl("https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=800&q=80");
        s6.addHighlight("Desayunos de autor servidos al amanecer", "Huevos benedictinos, bollería recién horneada y zumos naturales.");
        s6.addHighlight("Carta nocturna de platos calientes", "Opciones gastronómicas reconfortantes disponibles toda la noche.");
        s6.addHighlight("Servicio de champán y caviar", "Presentación en hielo con copas de cristal de Baccarat.");
        s6.addHighlight("Atención personalizada y discreta", "Montaje de mesa completo en la terraza o salón de la suite.");
        s6.addGalleryImage("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80");
        s6.addGalleryImage("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80");
        s6.addGalleryImage("https://images.unsplash.com/photo-1482049016688-2d3e1b311543?auto=format&fit=crop&w=800&q=80");
        save(s6);

        // 7. Servicio de lavandería (EXPERIENCIA 07)
        Service s7 = new Service(
            7L,
            "EXPERIENCIA 07",
            "Servicio de lavandería",
            "Lavandería y tintorería exprés con una presentación sobria, cuidada y acorde con la experiencia premium del hotel.",
            "7:00 AM - 7:00 PM",
            30.0,
            "Desde € 30 EUR",
            "https://images.unsplash.com/photo-1582735689369-4fe89db7114c?auto=format&fit=crop&w=800&q=80"
        );
        s7.setHeroDescription("Cuidado minucioso para tus prendas más delicadas. Técnicas ecológicas de planchado y tintorería artesanal entregadas con funda protectora en tu armario.");
        s7.setTagline("EXPRESS VALET & DRY CLEANING");
        s7.setHeadline("El cuidado más exigente para tus mejores prendas");
        s7.setFullDescription("Tratamiento textil de alta gama para seda, lino, lana virgen y trajes a medida, con servicio exprés en el mismo día y plegado en papel de seda.");
        s7.setScheduleNote("Lunes a Domingo");
        s7.setPriceNote("Por prenda o servicio completo");
        s7.setSecondaryImageUrl("https://images.unsplash.com/photo-1545173168-9f1947eebb7f?auto=format&fit=crop&w=800&q=80");
        s7.addHighlight("Servicio exprés en 4 horas", "Recogida y entrega directa en tu habitación.");
        s7.addHighlight("Tratamientos eco-friendly", "Productos hipoalergénicos y técnicas sin químicos agresivos.");
        s7.addHighlight("Planchado artesanal al vapor", "Cuidado especializado para trajes de noche y camisas de etiqueta.");
        s7.addHighlight("Presentación impecable", "Perchas de madera forrada y fundas protectoras transpirables.");
        s7.addGalleryImage("https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=800&q=80");
        s7.addGalleryImage("https://images.unsplash.com/photo-1604335399105-a0c585fd81a1?auto=format&fit=crop&w=800&q=80");
        s7.addGalleryImage("https://images.unsplash.com/photo-1582735689369-4fe89db7114c?auto=format&fit=crop&w=800&q=80");
        save(s7);

        // 8. Traslados (EXPERIENCIA 08)
        Service s8 = new Service(
            8L,
            "EXPERIENCIA 08",
            "Traslados",
            "Servicio privado con vehículos premium y chauffeur para desplazamientos por Mónaco y la Costa Azul.",
            "24 horas",
            0.0,
            "A consultar",
            "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80"
        );
        s8.setHeroDescription("Flota exclusiva de berlinas y minivans de alta gama con chóferes multilingües para traslados al Aeropuerto de Niza, helipuerto y destinos exclusivos de la Riviera.");
        s8.setTagline("CHAUFFEUR & LUXURY FLEET");
        s8.setHeadline("Movilidad de lujo con puntualidad y distinción absoluta");
        s8.setFullDescription("Viaja con la máxima serenidad y confort en vehículos equipados con Wi-Fi de alta velocidad, agua mineral artesanal y atención a cada uno de tus itinerarios.");
        s8.setScheduleNote("Reserva 24/7 previa");
        s8.setPriceNote("Tarifas fijas por trayecto");
        s8.setSecondaryImageUrl("https://images.unsplash.com/photo-1563720223185-11003d516935?auto=format&fit=crop&w=800&q=80");
        s8.addHighlight("Flota Mercedes-Benz Clase S y Maybach", "Vehículos insonorizados con tapicería de cuero nappa.");
        s8.addHighlight("Chóferes profesionales y bilingües", "Conocimiento experto de las rutas y protocolos de la Riviera.");
        s8.addHighlight("Conexión directa Aeropuerto Niza-Costa Azul", "Recepción personalizada en terminal con cartel y asistencia de equipaje.");
        s8.addHighlight("Servicio a disposición por horas", "Flexibilidad absoluta para compras, reuniones o paseos nocturnos.");
        s8.addGalleryImage("https://images.unsplash.com/photo-1502877338535-766e1452684a?auto=format&fit=crop&w=800&q=80");
        s8.addGalleryImage("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80");
        s8.addGalleryImage("https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80");
        save(s8);
    }

    public List<Service> findAll() {
        List<Service> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(Service::getId));
        return list;
    }

    public Optional<Service> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Service save(Service service) {
        if (service.getId() == null) {
            long next = store.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1L;
            service.setId(next);
        }
        store.put(service.getId(), service);
        return service;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public void deleteAll() {
        store.clear();
    }
}
