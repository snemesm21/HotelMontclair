package com.example.demo.service;

import com.example.demo.entities.TouristPlace;
import com.example.demo.repository.TouristPlaceRepository;
import java.util.List;
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
        for (PlaceSeed seed : initialPlaces()) {
            if (!placeRepository.existsBySlug(seed.slug())) {
                TouristPlace place = new TouristPlace();
                place.setSlug(seed.slug());
                place.setName(seed.name());
                place.setDescription(seed.description());
                place.setCategory(seed.category());
                place.setImageUrls(List.of(seed.imageUrl()));
                place.setAddress(seed.address());
                place.setLatitude(seed.latitude());
                place.setLongitude(seed.longitude());
                place.setSuggestedDurationMinutes(seed.durationMinutes());
                place.setActive(true);
                placeRepository.save(place);
            }
        }
    }

    private List<PlaceSeed> initialPlaces() {
        return List.of(
                new PlaceSeed("casino-de-monte-carlo", "Casino de Monte-Carlo",
                        "Icono de la Belle Époque diseñado por Charles Garnier, rodeado por la Place du Casino y sus jardines.",
                        "CULTURA", "https://commons.wikimedia.org/wiki/Special:FilePath/Monte_Carlo_Casino.jpg",
                        "Place du Casino, 98000 Monaco", 43.7396, 7.4273, 90),
                new PlaceSeed("palacio-del-principe", "Palacio del Príncipe de Mónaco",
                        "Residencia oficial del Príncipe de Mónaco. Sus apartamentos de Estado abren al público por temporada.",
                        "HISTORIA", "https://commons.wikimedia.org/wiki/Special:FilePath/Palais_princier_de_Monaco.jpg",
                        "Place du Palais, 98015 Monaco", 43.7311, 7.4204, 90),
                new PlaceSeed("puerto-hercules", "Puerto Hércules",
                        "Puerto deportivo histórico de aguas profundas, con vistas a Port Hercule y al circuito urbano del Gran Premio.",
                        "PASEO", "https://commons.wikimedia.org/wiki/Special:FilePath/Port_Hercule_Monaco.jpg",
                        "Port Hercule, 98000 Monaco", 43.7347, 7.4243, 60),
                new PlaceSeed("museo-oceanografico", "Museo Oceanográfico de Mónaco",
                        "Museo y acuario fundado por el Príncipe Alberto I, situado sobre el acantilado de Le Rocher.",
                        "MUSEO", "https://commons.wikimedia.org/wiki/Special:FilePath/Oceanographic_Museum_Monaco.jpg",
                        "Avenue Saint-Martin, 98000 Monaco", 43.7290, 7.4244, 150),
                new PlaceSeed("jardin-exotique", "Jardín Exótico de Mónaco",
                        "Jardín en altura conocido por su colección de plantas suculentas y la cueva del Observatorio.",
                        "NATURALEZA", "https://commons.wikimedia.org/wiki/Special:FilePath/Jardin_exotique_de_Monaco.jpg",
                        "Boulevard du Jardin Exotique, 98000 Monaco", 43.7322, 7.4145, 120),
                new PlaceSeed("catedral-de-monaco", "Catedral de Nuestra Señora Inmaculada",
                        "Catedral románico-bizantina del barrio de Monaco-Ville y lugar de sepultura de varios miembros de la familia principesca.",
                        "HISTORIA", "https://commons.wikimedia.org/wiki/Special:FilePath/Monaco_Cathedral.jpg",
                        "4 Rue Colonel Bellando de Castro, 98000 Monaco", 43.7308, 7.4213, 45),
                new PlaceSeed("jardin-japones", "Jardín Japonés de Mónaco",
                        "Jardín paisajístico de inspiración japonesa diseñado con estanque, cascada, puentes y casa de té.",
                        "NATURALEZA", "https://commons.wikimedia.org/wiki/Special:FilePath/Japanese_Garden_Monaco.jpg",
                        "Avenue Princesse Grace, 98000 Monaco", 43.7416, 7.4291, 45),
                new PlaceSeed("playa-larvotto", "Playa de Larvotto",
                        "Playa pública urbana de Mónaco, con paseo marítimo y acceso a servicios de playa.",
                        "PLAYA", "https://commons.wikimedia.org/wiki/Special:FilePath/Larvotto_beach_Monaco.jpg",
                        "Avenue Princesse Grace, 98000 Monaco", 43.7474, 7.4358, 90),
                new PlaceSeed("villa-sauber", "Villa Sauber - Nouveau Musée National de Monaco",
                        "Villa histórica que acoge exposiciones temporales de arte contemporáneo y patrimonio monegasco.",
                        "MUSEO", "https://commons.wikimedia.org/wiki/Special:FilePath/Villa_Sauber_Monaco.jpg",
                        "17 Avenue Princesse Grace, 98000 Monaco", 43.7436, 7.4306, 75),
                new PlaceSeed("fort-antoine", "Fort Antoine",
                        "Fortaleza del siglo XVIII convertida en teatro al aire libre, en el extremo de Monaco-Ville.",
                        "HISTORIA", "https://commons.wikimedia.org/wiki/Special:FilePath/Fort_Antoine_Monaco.jpg",
                        "Avenue de la Quarantaine, 98000 Monaco", 43.7322, 7.4187, 40),
                new PlaceSeed("jardines-saint-martin", "Jardines Saint-Martin",
                        "Jardines públicos en el acantilado de Le Rocher, con senderos y vistas al Mediterráneo.",
                        "NATURALEZA", "https://commons.wikimedia.org/wiki/Special:FilePath/Jardins_Saint-Martin_Monaco.jpg",
                        "Avenue Saint-Martin, 98000 Monaco", 43.7297, 7.4231, 45),
                new PlaceSeed("jardines-fontvieille", "Parque paisajístico de Fontvieille",
                        "Zona verde junto al puerto de Fontvieille, con paseo, esculturas y acceso al rosetón de la Princesa Grace.",
                        "NATURALEZA", "https://commons.wikimedia.org/wiki/Special:FilePath/Fontvieille_Monaco.jpg",
                        "Avenue des Guelfes, 98000 Monaco", 43.7269, 7.4183, 60)
        );
    }

    private record PlaceSeed(
            String slug,
            String name,
            String description,
            String category,
            String imageUrl,
            String address,
            double latitude,
            double longitude,
            int durationMinutes) {
    }
}
