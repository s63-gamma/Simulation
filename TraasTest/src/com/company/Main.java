package com.company;

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.ws.container.SumoStringList;
import it.polito.appeal.traci.SumoTraciConnection;

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

    static String sumo_bin = "C:/Users/Martijn/Documents/SUMO/sumo-0.29.0/bin/sumo-gui.exe";
    static String config_file = "C:/Users/Martijn/Documents/Traas/Example/quickstart.sumo.cfg";
    static double step_length = 0.5;

    public static void main(String[] args) {

        //start Simulation
        SumoTraciConnection conn = new SumoTraciConnection(sumo_bin, config_file);

        //set some options
        conn.addOption("step-length", "1"); //timestep 100 ms

        try{

            //start TraCI
            conn.runServer();

            //load routes and initialize the simulation
            conn.do_timestep();
            Random rand = new Random();
            for(int i=0; i<100; i++){
                //current simulation time
                //int simtime = (int) conn.do_job_get(Simulation.getCurrentTime());
//
//                conn.do_job_set(Vehicle.add("veh"+i, "car",  "" + rand.nextInt(1275), simtime, 0, 13.8, (byte) 1));

                conn.do_timestep();
                for (String vehicle : ((SumoStringList) conn.do_job_get(Simulation.getDepartedIDList()))) {
                    System.out.println(conn.do_job_get(Vehicle.getDistance(vehicle)));
                }
            }

            //stop TraCI
            conn.close();

        }catch(Exception ex){ex.printStackTrace();}

    }
}
