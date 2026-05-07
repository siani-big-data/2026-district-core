package siani.districting.readers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PopulationGenerator {

    private PopulationGenerator() {
    }

    public static List<Integer> generatePopulationPerPrecinct(
            int numPrecincts,
            int totalPopulation,
            long seed
    ) {
        if (numPrecincts <= 0) {
            throw new IllegalArgumentException("numPrecints must be greater than 0.");
        }
        if (totalPopulation < numPrecincts * 500) {
            throw new IllegalArgumentException("totalPopulation must be at least numPrecincts * 500.");
        }

        Random random = new Random(seed);

        final int MIN_POPULATION = 500;
        final int MAX_POPULATION = 3000;
        final double MEDIA = (double) totalPopulation / numPrecincts;

        List<Double> pesos = new ArrayList<>();

        for (int i = 0; i < numPrecincts; i++) {
            double factor = 0.65 + random.nextDouble() * 0.70;

            double r = random.nextDouble();
            if (r < 0.08) {
                factor *= 0.45 + random.nextDouble() * 0.35;
            } else if (r > 0.92) {
                factor *= 1.30 + random.nextDouble() * 0.60;
            }

            pesos.add(MEDIA * factor);
        }

        double sumaPesos = pesos.stream().mapToDouble(Double::doubleValue).sum();

        List<Integer> poblaciones = new ArrayList<>();
        int sumaActual = 0;

        for (double peso : pesos) {
            int valor = (int) Math.round((peso / sumaPesos) * totalPopulation);
            valor = Math.max(MIN_POPULATION, Math.min(MAX_POPULATION, valor));
            poblaciones.add(valor);
            sumaActual += valor;
        }

        int diferencia = totalPopulation - sumaActual;
        int intentos = 0;

        while (diferencia != 0 && intentos < numPrecincts * 20) {
            int idx = random.nextInt(numPrecincts);
            int actual = poblaciones.get(idx);

            if (diferencia > 0 && actual < MAX_POPULATION) {
                poblaciones.set(idx, actual + 1);
                diferencia--;
            } else if (diferencia < 0 && actual > MIN_POPULATION) {
                poblaciones.set(idx, actual - 1);
                diferencia++;
            }

            intentos++;
        }

        return poblaciones;
    }
}
