import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

//retrieve weather data from API - backend logic that will fetch the latest weather data
public class WeatherApp {
    //fetch weather data for given location
    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        //extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
        "latitude=" + latitude + "&longitude=" + longitude +
        "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Europe/Sofia";

        try {
            //call api and get response
            HttpURLConnection connection = fetchApiResponse(urlString);

            // check for response status
            if(connection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(connection.getInputStream());
            while(scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            connection.disconnect();

            //parse through the data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            //retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            //het the index of our current hour
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexofCurrentTime(time);

            //get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            //get weather code
            JSONArray weatherCode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weatherCode.get(index));

            // get humidity data
            JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            //get windspeed
            JSONArray windSpeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windSpeed = (double) windSpeedData.get(index);

            //build the weather json data object
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windSpeed);

            return weatherData;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static JSONArray getLocationData(String locationName) {
        // replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try {
            //call api and get a response
            HttpURLConnection connection = fetchApiResponse(urlString);

            //check response status - 200/400/500
            if(connection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            } else {
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(connection.getInputStream());

                // read and store resulting json data into our string builder
                while(scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();
                connection.disconnect();

                // parse the json string into a Json obj
                JSONParser parser = new JSONParser();
                JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                //get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultJsonObj.get("results");
                return  locationData;
            }


        } catch(Exception e) {
            e.printStackTrace();
        }
        //couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //set request method to get
            connection.setRequestMethod("GET");

            //connect to our API
            connection.connect();
            return connection;
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int findIndexofCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        // iterate over hte time list and see which one matches our current time
        for(int i = 0; i< timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)) {
                //return index
                return i;
            }
        }

        return 0;
    }

    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();

        // format date ex: 2024-07-09T00:00 (this is how it's read from the API
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //format and print
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    private static String convertWeatherCode(long weatherCode) {
        String weatherCondition = "";
        if(weatherCode == 0L) {
            weatherCondition = "Clear";
        } else if(weatherCode <= 3L && weatherCode > 0L) {
            weatherCondition = "Cloudy";
        } else if((weatherCode >= 51L && weatherCode <= 67L)
                || (weatherCode >= 80L && weatherCode <= 99L)) {
            //rain
            weatherCondition = "Rain";
        } else if(weatherCode >= 71L && weatherCode <= 77L) { //rain and snow code checks are generalized for simplicity
            //snow
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
