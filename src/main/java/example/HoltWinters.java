package example;

/**
 * Created by sgoyal5 on 7/12/15.
 */

public class HoltWinters {



    public static double[] doubleExponentialForecast(double[] data, double alpha, double gamma, int initializationMethod, int numForecasts) {
        double[] y = new double[data.length + numForecasts];
        double[] s = new double[data.length];
        double[] b = new double[data.length];
        s[0] = y[0] = data[0];

        if(initializationMethod==0) {
            b[0] = data[1]-data[0];
        } else if(initializationMethod==1 && data.length>4) {
            b[0] = (data[3] - data[0]) / 3;
        } else if(initializationMethod==2) {
            b[0] = (data[data.length - 1] - data[0])/(data.length - 1);
        }

        int i = 1;
        y[1] = s[0] + b[0];
        for (i = 1; i < data.length; i++) {
            s[i] = alpha * data[i] + (1 - alpha) * (s[i - 1]+b[i - 1]);
            b[i] = gamma * (s[i] - s[i - 1]) + (1-gamma) * b[i-1];
            y[i+1] = s[i] + b[i];
        }

        for (int j = 0; j < numForecasts ; j++, i++) {
            y[i] = s[data.length-1] + (j+1) * b[data.length-1];
        }

        return y;
    }
    /**
     * This method is the entry point. It calculates the initial values and returns the forecast
     * for the m periods.
     *
     * @param y - Time series data.
     * @param alpha - Exponential smoothing coefficients for level, trend, seasonal components.
     * @param beta - Exponential smoothing coefficients for level, trend, seasonal components.
     * @param gamma - Exponential smoothing coefficients for level, trend, seasonal components.
     * @param period - A complete season's data consists of L periods. And we need to estimate
     * the trend factor from one period to the next. To accomplish this, it is advisable to use
     * two complete seasons; that is, 2L periods.
     * @param m - Extrapolated future data points.
     * @param debug - Print debug values. Useful for testing.
     *
     *				4 quarterly
     *     			7 weekly.
     *     			12 monthly
     */

    public static double[] forecast(int[] y, double alpha, double beta,
                                    double gamma, int period, int m, boolean debug) {

        if (y == null) {
            return null;
        }

        int seasons = y.length / period;
        double a0 = calculateInitialLevel(y, period);
        double b0 = calculateInitialTrend(y, period);
        double[] initialSeasonalIndices = calculateSeasonalIndices(y, period, seasons);

        if (debug) {
            System.out.println(String.format(
                    "Total observations: %d, Seasons %d, Periods %d", y.length,
                    seasons, period));
            System.out.println("Initial level value a0: " + a0);
            System.out.println("Initial trend value b0: " + b0);
            printArray("Seasonal Indices: ", initialSeasonalIndices);
        }

        double[] forecast = calculateHoltWinters(y, a0, b0, alpha, beta, gamma,
                initialSeasonalIndices, period, m, debug);

        if (debug) {
            printArray("Forecast", forecast);
        }

        return forecast;
    }


    /**
     * This method realizes the Holt-Winters equations.
     *
     * @param y
     * @param a0
     * @param b0
     * @param alpha
     * @param beta
     * @param gamma
     * @param initialSeasonalIndices
     * @param period
     * @param m
     * @param debug
     * @return - Forecast for m periods.
     */
    private static double[] calculateHoltWinters(int[] y, double a0, double b0, double alpha,
                                                 double beta, double gamma, double[] initialSeasonalIndices, int period, int m, boolean debug) {

        double[] St = new double[y.length];
        double[] Bt = new double[y.length];
        double[] It = new double[y.length];
        double[] Ft = new double[y.length + m];

        //Initialize base values
        St[1] = a0;
        Bt[1] = b0;

        for (int i = 0; i < period; i++) {
            It[i] = initialSeasonalIndices[i];
        }

        Ft[m] = (St[0] + (m * Bt[0])) * It[0];//This is actually 0 since Bt[0] = 0
        Ft[m + 1] = (St[1] + (m * Bt[1])) * It[1];//Forecast starts from period + 2

        //Start calculations
        for (int i = 2; i < y.length; i++) {

            //Calculate overall smoothing
            if((i - period) >= 0) {
                St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1]);
            } else {
                St[i] = alpha * y[i] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1]);
            }

            //Calculate trend smoothing
            Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1];

            //Calculate seasonal smoothing
            if((i - period) >= 0) {
                It[i] = beta * y[i] / St[i] + (1.0 - beta) * It[i - period];
            }

            //Calculate forecast
            if( ((i + m) >= period) ){
                Ft[i + m] = (St[i] + (m * Bt[i])) * It[i - period + m];
            }

            if(debug){
                System.out.println(String.format(
                        "i = %d, y = %d, S = %f, Bt = %f, It = %f, F = %f", i,
                        y[i], St[i], Bt[i], It[i], Ft[i]));
            }
        }

        return Ft;
    }


    private static double calculateInitialLevel(int[] y, int period) {


        return y[0];
    }


    private static double calculateInitialTrend(int[] y, int period){

        double sum = 0;

        for (int i = 0; i < period; i++) {
            sum += (y[period + i] - y[i]);
        }

        return sum / (period * period);
    }


    private static double[] calculateSeasonalIndices(int[] y, int period, int seasons){

        double[] seasonalAverage = new double[seasons];
        double[] seasonalIndices = new double[period];

        double[] averagedObservations = new double[y.length];

        for (int i = 0; i < seasons; i++) {
            for (int j = 0; j < period; j++) {
                seasonalAverage[i] += y[(i * period) + j];
            }
            seasonalAverage[i] /= period;
        }

        for (int i = 0; i < seasons; i++) {
            for (int j = 0; j < period; j++) {
                averagedObservations[(i * period) + j] = y[(i * period) + j] / seasonalAverage[i];
            }
        }

        for (int i = 0; i < period; i++) {
            for (int j = 0; j < seasons; j++) {
                seasonalIndices[i] += averagedObservations[(j * period) + i];
            }
            seasonalIndices[i] /= seasons;
        }

        return seasonalIndices;
    }


    private static void printArray(String description, double[] data){

        System.out.println(String.format("******************* %s *********************", description));

        for (int i = 0; i < data.length; i++) {
            System.out.println(data[i]);
        }

        System.out.println(String.format("*****************************************************************", description));
    }
}

