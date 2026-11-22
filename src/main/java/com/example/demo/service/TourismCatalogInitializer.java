package com.example.demo.service;

import com.example.demo.entities.TouristPlace;
import com.example.demo.repository.TouristPlaceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TourismCatalogInitializer implements ApplicationRunner {

    private final TouristPlaceRepository placeRepository;

    public TourismCatalogInitializer(TouristPlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        List<PlaceSeed> seeds = initialPlaces();
        Set<String> seedSlugs = seeds.stream().map(PlaceSeed::slug).collect(Collectors.toSet());

        for (PlaceSeed seed : seeds) {
            TouristPlace place = placeRepository.findBySlug(seed.slug()).orElseGet(TouristPlace::new);
            place.setSlug(seed.slug());
            place.setName(seed.name());
            place.setLongDescription(seed.description());
            place.setShortDescription(seed.shortDescription());
            place.setCategory(seed.category());
            place.setImageUrls(seed.imageUrl().isBlank() ? List.of() : List.of(seed.imageUrl()));
            place.setImageAttributions(
                    seed.imageAttribution().isBlank() ? List.of() : List.of(seed.imageAttribution()));
            place.setAddress(seed.address());
            place.setLatitude(seed.latitude());
            place.setLongitude(seed.longitude());
            place.setSuggestedDurationMinutes(seed.durationMinutes());
            place.setDisplayOrder(seed.displayOrder());
            place.setActive(true);
            placeRepository.save(place);
        }

        // Desactivar lugares legados que no pertenezcan al catálogo oficial de 25 lugares
        for (TouristPlace place : placeRepository.findAll()) {
            if (!seedSlugs.contains(place.getSlug())) {
                place.setActive(false);
                placeRepository.save(place);
            }
        }
    }

    private List<PlaceSeed> initialPlaces() {
        return List.of(
                // ==========================================
                // 1. LUGARES ICÓNICOS (5 LUGARES)
                // ==========================================
                new PlaceSeed("casino-de-monte-carlo", "Casino de Monte-Carlo",
                        "Icono de la Belle Époque diseñado por Charles Garnier, rodeado por la Place du Casino y sus jardines.",
                        "Icono de la Belle Époque junto a la Place du Casino.",
                        "LUGARES ICÓNICOS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ac/Casino_de_Montecarlo%2C_M%C3%B3naco%2C_2016-06-23%2C_DD_06.jpg/960px-Casino_de_Montecarlo%2C_M%C3%B3naco%2C_2016-06-23%2C_DD_06.jpg",
                        "Diego Delso, Casino de Montecarlo, Mónaco, CC BY-SA 4.0",
                        "Place du Casino, 98000 Monaco", 43.7391605, 7.4280230, 90, 1),
                new PlaceSeed("puerto-hercules", "Puerto Hércules",
                        "Puerto deportivo histórico de aguas profundas, con vistas a Port Hercule y al circuito urbano del Gran Premio.",
                        "Paseo por el puerto histórico y sus muelles frente al Mediterráneo.",
                        "LUGARES ICÓNICOS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dc/Yachts_in_Port_Hercule%2C_Monaco_%28July_2020%29_01.jpg/960px-Yachts_in_Port_Hercule%2C_Monaco_%28July_2020%29_01.jpg",
                        "Renek78, Yachts in Port Hercule, CC BY-SA 4.0",
                        "Port Hercule, 98000 Monaco", 43.735406, 7.423145, 60, 2),
                new PlaceSeed("palacio-del-principe", "Palacio del Príncipe de Mónaco",
                        "Residencia oficial del Príncipe de Mónaco. Sus apartamentos de Estado abren al público por temporada.",
                        "Residencia oficial del Príncipe de Mónaco en Monaco-Ville.",
                        "LUGARES ICÓNICOS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e7/La_Palais_Princier_%28Monaco%29.jpg/960px-La_Palais_Princier_%28Monaco%29.jpg",
                        "Van Der Meulen Christofle, La Palais Princier, CC0",
                        "Place du Palais, 98000 Monaco", 43.7312602, 7.4198234, 90, 3),
                new PlaceSeed("museo-oceanografico", "Museo Oceanográfico de Mónaco",
                        "Museo y acuario fundado por el Príncipe Alberto I, situado sobre el acantilado de Le Rocher.",
                        "Museo y acuario histórico sobre el acantilado de Le Rocher.",
                        "LUGARES ICÓNICOS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/10/Oceanographic_Museum.jpg/960px-Oceanographic_Museum.jpg",
                        "Gleb Radchenko, Oceanographic Museum, CC BY-SA 3.0",
                        "Avenue Saint-Martin, 98000 Monaco", 43.7306805, 7.4255127, 150, 4),
                new PlaceSeed("curva-fairmont-f1", "Curva Fairmont del Gran Premio de F1",
                        "La horquilla más famosa y lenta de la historia de la Fórmula 1, rodeada por el ambiente electrizante de Monte-Carlo.",
                        "El tramo más legendario y fotografiado del circuito callejero de Mónaco.",
                        "LUGARES ICÓNICOS",
                        "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Monaco GP Legendary Hairpin",
                        "Avenue des Spélugues, 98000 Monaco", 43.7403, 7.4294, 45, 5),

                // ==========================================
                // 2. CULTURA E HISTORIA (5 LUGARES)
                // ==========================================
                new PlaceSeed("opera-de-monte-carlo", "Ópera de Monte-Carlo (Salle Garnier)",
                        "Joya arquitectónica de la Belle Époque con interiores dorados diseñada por Charles Garnier junto a la Place du Casino.",
                        "Joya de la Belle Époque y sede lírica de renombre mundial.",
                        "CULTURA E HISTORIA",
                        "https://images.unsplash.com/photo-1514306191717-452ec28c7814?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Opera Garnier Grand Hall",
                        "Place du Casino, 98000 Monaco", 43.738889, 7.428056, 90, 6),
                new PlaceSeed("catedral-de-monaco", "Catedral de Nuestra Señora Inmaculada",
                        "Catedral románico-bizantina del barrio de Monaco-Ville y lugar de sepultura de varios miembros de la familia principesca.",
                        "Catedral románico-bizantina del barrio de Monaco-Ville.",
                        "CULTURA E HISTORIA",
                        "https://images.unsplash.com/photo-1543783207-ec64e4d95325?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Monaco Historic Cathedral Architecture",
                        "Rue de l'Abbaye, 98000 Monaco", 43.7302470, 7.4226535, 45, 7),
                new PlaceSeed("coleccion-automoviles-principe", "Colección de Coches de S.A.S. el Príncipe",
                        "Extraordinaria colección de más de 100 automóviles clásicos, deportivos y bólidos de Fórmula 1 reunidos por la familia principesca.",
                        "Exposición principesca de bólidos de F1 y coches de época.",
                        "CULTURA E HISTORIA",
                        "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Monaco Luxury Sports and Classic Cars",
                        "54 Route de la Piscine, Port Hercule, 98000 Monaco", 43.7347, 7.4215, 90, 8),
                new PlaceSeed("fort-antoine", "Fort Antoine",
                        "Fortaleza del siglo XVIII convertida en teatro al aire libre, en el extremo oriental de Le Rocher.",
                        "Fortaleza histórica transformada en un teatro al aire libre.",
                        "CULTURA E HISTORIA",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/16/Fort_Antoine_-_panoramio.jpg/960px-Fort_Antoine_-_panoramio.jpg",
                        "MSzybalski, Fort Antoine, CC BY-SA 3.0",
                        "Théâtre du Fort Antoine, 98000 Monaco", 43.7330810, 7.4279432, 40, 9),
                new PlaceSeed("villa-sauber", "Villa Sauber - Nouveau Musée National",
                        "Villa histórica que acoge exposiciones temporales de arte contemporáneo y patrimonio monegasco.",
                        "Villa histórica dedicada a exposiciones del museo nacional de Mónaco.",
                        "CULTURA E HISTORIA",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Nouveau_mus%C3%A9e_national_de_Monaco_%28novembre_2021%29.jpg/960px-Nouveau_mus%C3%A9e_national_de_Monaco_%28novembre_2021%29.jpg",
                        "Benoît Prieur, Nouveau musée national de Monaco, CC0",
                        "17 Avenue Princesse Grace, 98000 Monaco", 43.7443936, 7.4304816, 75, 10),

                // ==========================================
                // 3. NATURALEZA Y PLAYAS (5 LUGARES)
                // ==========================================
                new PlaceSeed("jardin-exotique", "Jardín Exótico de Mónaco",
                        "Jardín en altura conocido por su colección de plantas suculentas y la cueva del Observatorio.",
                        "Jardín en altura conocido por su colección de plantas suculentas.",
                        "NATURALEZA Y PLAYAS",
                        "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Exotic Botanical Garden Monaco",
                        "62 Boulevard du Jardin Exotique, 98000 Monaco", 43.7312785, 7.4141717, 120, 11),
                new PlaceSeed("jardin-japones", "Jardín Japonés de Mónaco",
                        "Jardín paisajístico de inspiración japonesa diseñado con estanque, cascada, puentes y casa de té.",
                        "Jardín paisajístico con estanque, cascada, puentes y casa de té.",
                        "NATURALEZA Y PLAYAS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/01/Jardin_Japonais_-_monaco_-_panoramio.jpg/960px-Jardin_Japonais_-_monaco_-_panoramio.jpg",
                        "kajikawa, Jardin Japonais Monaco, CC BY 3.0",
                        "Avenue Mareterra, 98000 Monaco", 43.7422424, 7.4309103, 45, 12),
                new PlaceSeed("playa-larvotto", "Playa de Larvotto",
                        "Playa pública urbana de Mónaco, con paseo marítimo, aguas cristalinas y clubes costeros.",
                        "Playa pública urbana junto al paseo marítimo de Mónaco.",
                        "NATURALEZA Y PLAYAS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0e/Plage_de_Larvotto_%28Monaco%29_en_novembre_2021_%283%29.jpg/960px-Plage_de_Larvotto_%28Monaco%29_en_novembre_2021_%283%29.jpg",
                        "Benoît Prieur, Plage de Larvotto, CC0",
                        "Plage du Larvotto, 98000 Monaco", 43.7459457, 7.4330690, 90, 13),
                new PlaceSeed("jardines-saint-martin", "Jardines Saint-Martin",
                        "Jardines públicos en el acantilado de Le Rocher, con senderos sombreados y vistas al mar.",
                        "Jardines públicos en el acantilado de Le Rocher con vistas al Mediterráneo.",
                        "NATURALEZA Y PLAYAS",
                        "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Jardins_Saint-Martin%2C_Monaco_-_panoramio_%281%29.jpg/960px-Jardins_Saint-Martin%2C_Monaco_-_panoramio_%281%29.jpg",
                        "M. Stricis, Jardins Saint-Martin, CC BY-SA 3.0",
                        "Jardins de Saint-Martin, Monaco-Ville, 98000 Monaco", 43.7300402, 7.4242832, 45, 14),
                new PlaceSeed("roseraye-princesse-grace", "Rosaleda Princesa Grace",
                        "Jardín botánico con más de 300 variedades de rosas y 6.000 rosales inaugurado en memoria de la Princesa Grace.",
                        "Romántica rosaleda paisajística con miles de rosas y esculturas.",
                        "NATURALEZA Y PLAYAS",
                        "https://images.unsplash.com/photo-1496062031456-07b8f162a322?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Princess Grace Rose Garden Roses",
                        "Avenue des Guelfes, 98000 Monaco", 43.7271, 7.4194, 45, 15),

                // ==========================================
                // 4. GASTRONOMÍA (5 LUGARES)
                // ==========================================
                new PlaceSeed("le-louis-xv-alain-ducasse", "Le Louis XV - Alain Ducasse",
                        "Emblema culinario supremo con tres estrellas Michelin que exalta los productos del Mediterráneo en el Hôtel de Paris.",
                        "Templo gastronómico con tres estrellas Michelin inspirado en la Riviera.",
                        "GASTRONOMÍA",
                        "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Luxury Fine Dining Monaco",
                        "Place du Casino, 98000 Monaco", 43.7391, 7.4276, 150, 16),
                new PlaceSeed("cafe-de-paris-monte-carlo", "Café de Paris Monte-Carlo",
                        "Mítica brasserie de estilo parisino con terraza legendaria para disfrutar de mariscos y cócteles en la Place du Casino.",
                        "Legendaria brasserie monegasca con terraza frente al Casino.",
                        "GASTRONOMÍA",
                        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Café de Paris Monte Carlo",
                        "Place du Casino, 98000 Monaco", 43.7395, 7.4282, 75, 17),
                new PlaceSeed("le-grill-hotel-de-paris", "Le Grill - Hôtel de Paris",
                        "Restaurante con una estrella Michelin situado en el 8.º piso con carnes a la brasa y techo corredizo panorámico.",
                        "Estrella Michelin en las alturas con techo retráctil bajo las estrellas.",
                        "GASTRONOMÍA",
                        "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Panoramic Michelin Restaurant",
                        "Place du Casino, 98000 Monaco", 43.7392, 7.4277, 120, 18),
                new PlaceSeed("maya-bay-monaco", "Maya Bay",
                        "Espacio de alta gastronomía asiática que une con maestría los sabores de Japón y Tailandia en un ambiente zen.",
                        "Alta cocina asiática refinada en el paseo de Larvotto.",
                        "GASTRONOMÍA",
                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Asian Fine Dining Monaco",
                        "24 Avenue Princesse Grace, 98000 Monaco", 43.7461, 7.4338, 90, 19),
                new PlaceSeed("marche-de-la-condamine", "Marché de la Condamine",
                        "Mercado histórico y gastronómico cubierto donde saborear especialidades autóctonas como el barbagiuan y la socca.",
                        "Mercado gourmet tradicional con especialidades típicas monegascas.",
                        "GASTRONOMÍA",
                        "https://images.unsplash.com/photo-1533900298318-6b8da08a523e?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Condamine Gourmet Market",
                        "Place d'Armes, 98000 Monaco", 43.7348, 7.4187, 60, 20),

                // ==========================================
                // 5. EXPERIENCIAS Y PASEOS (5 LUGARES)
                // ==========================================
                new PlaceSeed("champions-promenade", "Paseo de los Campeones",
                        "Paseo marítimo que inmortaliza las huellas en bronce de las leyendas mundiales del fútbol frente al mar.",
                        "Paseo con las huellas de las mayores leyendas del deporte.",
                        "EXPERIENCIAS Y PASEOS",
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Seaside Promenade",
                        "Promenade Princesse Grace, 98000 Monaco", 43.7433, 7.4322, 45, 21),
                new PlaceSeed("callejuelas-le-rocher", "Callejuelas Medievales de Le Rocher",
                        "Paseo peatonal entre callejones adoquinados, casas históricas color pastel y miradores de la ciudadela antigua.",
                        "Recorrido a pie por la villa medieval fortificada de Le Rocher.",
                        "EXPERIENCIAS Y PASEOS",
                        "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Le Rocher Old Town Walk",
                        "Place de la Mairie, Monaco-Ville, 98000 Monaco", 43.7308, 7.4219, 60, 22),
                new PlaceSeed("puerto-de-fontvieille", "Puerto de Fontvieille",
                        "Tranquila marina deportiva al abrigo del Peñón con muelles peatonales, palmeras y terrazas marineras.",
                        "Marina deportiva rodeada de terrazas al abrigo del Peñón.",
                        "EXPERIENCIAS Y PASEOS",
                        "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Fontvieille Monaco Marina and Cliffs",
                        "Quai Jean-Charles Rey, 98000 Monaco", 43.7291, 7.4183, 50, 23),
                new PlaceSeed("mirador-place-du-palais", "Mirador de la Place du Palais",
                        "Explanada histórica con cañones de época y una vista panorámica de 360 grados sobre Port Hercule y Monte-Carlo.",
                        "Balcón panorámico con vistas inigualables a Port Hercule y Monte-Carlo.",
                        "EXPERIENCIAS Y PASEOS",
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Panorama of Port Hercule from Le Rocher",
                        "Place du Palais, 98000 Monaco", 43.7315, 7.4206, 40, 24),
                new PlaceSeed("jardins-petite-afrique", "Jardines de la Pequeña África",
                        "Frondoso oasis botánico del siglo XIX con especies tropicales, ficus centenarios y estanques junto al Casino.",
                        "Oasis botánico tropical del siglo XIX con cascadas y flora exótica.",
                        "EXPERIENCIAS Y PASEOS",
                        "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=960&q=80",
                        "Unsplash - Casino Gardens Botanical Park",
                        "Place du Casino, 98000 Monaco", 43.7397, 7.4275, 40, 25)
        );
    }

    private record PlaceSeed(
            String slug,
            String name,
            String description,
            String shortDescription,
            String category,
            String imageUrl,
            String imageAttribution,
            String address,
            double latitude,
            double longitude,
            int durationMinutes,
            int displayOrder) {
    }
}
