package br.com.apollomusic.app.infra.config;

public class LocalizationUtils {
    public static Double calculeDistance(String latitudeA, String longitudeA, String latitudeB, String longitudeB ){
        final int R = 6371;
        Double lat1 = Double.parseDouble(latitudeA);
        Double lon1 = Double.parseDouble(longitudeA);
        Double lat2 = Double.parseDouble(latitudeB);
        Double lon2 = Double.parseDouble(longitudeB);
        Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(lon2-lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
        return distance;
    }

    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }
}
