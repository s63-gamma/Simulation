package main.java.com.company;

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.ws.container.SumoStringList;
import it.polito.appeal.traci.SumoTraciConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;


/*
Copyright (C) 2013 Mario Krumnow, Dresden University of Technology

This file is part of TraaS.

TraaS is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License.

TraaS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with TraaS.  If not, see <http://www.gnu.org/licenses/>.
*/

public class Main {

    static String projectPath = System.getProperty("user.dir").replace("\\", "/").substring(0, System.getProperty("user.dir").lastIndexOf("\\"));
    static String sumo_bin = projectPath + "/SUMO/sumo-0.29.0/bin/sumo-gui.exe";
    static String config_file = projectPath + "/Bicester/Bicester.sumo.cfg";
    static double step_length = 0.5;

    private static double startPositionX = 7029.01;
    private static double startPositionY = 8172.60;
    private static double startPositionLat = 51.900739;
    private static double startPositionLon = -1.153807;

    private static double latChangePerX = -0.0000002246666666666667;
    private static double lonChangePerX = 0.00001452766666666667;
    private static double latChangePerY = 0.000008987333333333333;
    private static double lonChangePerY = 0.0000003696666666666667;

    //Centre    X:7029.01 Y:8172.60  Lat:51.900739 Lon:-1.153807
    //3000 left X:4028.87 Y:8172.39  Lat:51.901413 Lon:-1.197390
    //3000 up   X:7029.15 Y:11172,87 Lat:51.927701 Lon:-1.152698

    private static HashMap<String, Integer> vehicles = new HashMap<String, Integer>();

    private static Boolean isPostingNewVehicles = false;
    private static Boolean isPostingPositions = false;

    private static int maxAmountCars = 5;
    private static int timeStepBeforeGPSPost = 5;

    public static void main(String[] args) {
        //start Simulation
        SumoTraciConnection conn = new SumoTraciConnection(sumo_bin, config_file);

        //set some options
        conn.addOption("step-length", "0.05"); //timestep 100 ms

        try{
            //start TraCI
            conn.runServer();

            //load routes and initialize the simulation
            conn.do_timestep();

            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost;

            Random random = new Random();

            for(int i=0; i<10; i++){
                //current simulation time.
                int simtime = Integer.parseInt("" + conn.do_job_get(Simulation.getCurrentTime()));

                //Adding vehicles.
                if(vehicles.size() < maxAmountCars)
                    conn.do_job_set(Vehicle.add(generateLicensePlate(), "car", "" + random.nextInt(100), simtime, 0, 0, (byte) 0));

                //Adding newly departed vehicles.
                for (String vehicle : ((SumoStringList) conn.do_job_get(Simulation.getDepartedIDList()))) {
                    vehicles.put(vehicle, 0);

                    //Posting the new vehicles to the api.
                    if(isPostingNewVehicles) {
                        httppost = new HttpPost("https://api.guushamm.nl/car");

                        JSONObject json = new JSONObject();
                        json.put("buildingYear",  (1970 + random.nextInt(40)));
                        json.put("licensePlate", vehicle);
                        json.put("weigth",  (1500 + random.nextInt(1000)));
                        json.put("milage",  (100 + random.nextInt(10000)));
                        json.put("type", "car");
                        StringEntity input = new StringEntity(json.toString());
                        input.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                        httppost.setEntity(input);


                        HttpResponse response = httpclient.execute(httppost);
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            InputStream instream = entity.getContent();
                            try {
                                // do something useful
                            } finally {
                                instream.close();
                            }
                        }
                    }
                }

                if(i % timeStepBeforeGPSPost == 0) {
                    for (String vehicle : vehicles.keySet()) {

                        //Posting the positions of all the driving vehicles
                        if (isPostingPositions) {
                            httppost = new HttpPost("https://api.guushamm.nl/gpspoint");

                            String position = conn.do_job_get(Vehicle.getPosition(vehicle)).toString();
                            double positionX = Double.parseDouble(position.substring(0, position.indexOf(',') - 1));
                            double positionY = Double.parseDouble(position.substring(position.indexOf(',') + 1));
                            double lat = startPositionLat + (positionX - startPositionX) * latChangePerX + (positionY - startPositionY) * latChangePerY;
                            double lon = startPositionLon + (positionX - startPositionX) * lonChangePerX + (positionY - startPositionY) * lonChangePerY;

                            JSONObject json = new JSONObject();
                            json.put("longitude", lon);
                            json.put("latitude", lat);
                            json.put("sequenceNumber", vehicles.get(vehicle));
                            StringEntity input = new StringEntity(json.toString());
                            input.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                            httppost.setEntity(input);

                            HttpResponse response = httpclient.execute(httppost);
                            HttpEntity entity = response.getEntity();

                            vehicles.put(vehicle, vehicles.get(vehicle) + 1);

                            if (entity != null) {
                                InputStream instream = entity.getContent();
                                try {
                                    // do something useful
                                } finally {
                                    instream.close();
                                }
                            }
                        }
                    }
                }

                //Proceeding the simulation.
                conn.do_timestep();

                //Removing any arrived vehicles.
                for (String vehicle : ((SumoStringList) conn.do_job_get(Simulation.getArrivedIDList()))) {
                    vehicles.remove(vehicle);
                }

            }

            //stop TraCI
            conn.close();

        }catch(Exception ex){ex.printStackTrace();}

    }

    static String generateLetters(int amount) {
        String letters = "";
        int n = 'Z' - 'A' + 1;
        for (int i = 0; i < amount; i++) {
            char c = (char) ('A' + Math.random() * n);
            letters += c;
        }
        return letters;
    }

    static String generateDigits(int amount) {
        String digits = "";
        int n = '9' - '0' + 1;
        for (int i = 0; i < amount; i++) {
            char c = (char) ('0' + Math.random() * n);
            digits += c;
        }
        return digits;
    }

    static String generateLicensePlate() {
        String licensePlate;
        String letters;
        letters = generateLetters(3);
        String digits = generateDigits(3);

        licensePlate = letters + "-" + digits;
        return licensePlate;
    }
}
