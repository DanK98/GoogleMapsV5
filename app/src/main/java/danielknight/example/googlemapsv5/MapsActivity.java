package danielknight.example.googlemapsv5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    //Class Constants
    private static final int COLOR_BLACK_ARGB = 0xff000000; // defs Black
    private static final int COLOR_WHITE_ARGB = 0xffffffff; // defs White
    private static final int COLOR_GREEN_ARGB = 0xff00ff00; // defs Green
    private static final int COLOR_PURPLE_ARGB = 0xff800080; //defs Purple
    private static final int COLOR_ORANGE_ARGB = 0xffffa500; //defs Orange
    private static final int COLOR_BLUE_ARGB = 0xff0000ff; //defs Blue
    private static final int COLOR_RED_ARGB = 0xffFF0000; // defs Red
    private static final int COLOR_YELLOW_ARGB = 0xffFFFF00; //defs Yellow
    private static final int COLOR_PINK_ARGB = 0xffFFC0CB; //defs Pink (Error colour)
    private static final int polyLineWidth = 10; //Width of route polylines
    private static final int lowestZIndex = 0; // sets the polyline to lowest pos in stack
    private static final int highestZIndex = 1000; // sets the polyline to highest pos in stack
    private static final int cameraZoom = 15; // controls the pos of the camera above the polyline
    //File used to communicate between both apps. Must have same name
    // and fixed format(Defined elsewhere)
    private static final String cdDataFile = "crimeData.txt";
    private static final int maxNumberofRoutes = 5;

    //Error codes
    private static final int noError = 0;
    private static final int ErrorCode = -1;

    //Class Variables
    private static String apiKey = null;
    private static double circleSize = 0.0; // circle size in distanceUnits
    // this is the units of the distance given to a circle
    // read from the interface file, however defaults to K
    private static String distanceUnits = "K";
    private static String rangeData = null;
    private static int HeatMapIncrements = 0;
    private static myRangeDataClass rdc = null;
    private static String routeOrigin = null;
    private static String routeDestination = null;
    private static GoogleMap mMap;
    private static GoogleApiClient mGoogleApiClient; // depreciated
    private static GeoApiContext geoContext;
    // this is sampling size from the routes -- to reduce processing (see documentation)
    private static int routeStepSize = 50;
    private static int numberofRoutes = 0;



    //Class Complex Variables
    private static int[] heatmapColours;
    private static float[] heatmapStartPoint;
    private static int[] routeColourArray = {COLOR_BLACK_ARGB, COLOR_BLUE_ARGB, COLOR_GREEN_ARGB,
            COLOR_ORANGE_ARGB, COLOR_PURPLE_ARGB};
    private static ArrayList<myCircleClass> MyCircles = new ArrayList<>();
    private static ArrayList<myRangeDataClass> myRangeData = new ArrayList<>();
    private static DirectionsResult myRoutes;
    private static ArrayList<ArrayList<LatLng>> decodedPathArray=new ArrayList<>(maxNumberofRoutes);
    private static double routeRisk[];


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION IS THE SUBCLASSES
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * myCircleClass
     *
     *  FUNCTION DESCRIPTION
     *      This static class is the type that stores the crime circles
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public static class myCircleClass
    {
        public LatLng Centre;
        public Double Radius;
        public String Colour;
        public double circleWeight;

        /**
         * -----------------------------------------------------------------------------------------
         * myCircleClass(LatLng c, double r, String cl, String t)
         *
         *  CLASS DESCRIPTION
         *      this is a represnstation of a crime circle of radius r based round
         *      center c and having a severity weight of t)
         *
         *      colour is provided in case circles are drawn. (see documentation)
         *
         * @author Daniel Knight
         * @version 1.0
         * @since 30/07/2021
         * ---------------------------------------------------
         * @param c
         * @param r
         * @param cl
         * @param cw
         */
        public myCircleClass(LatLng c, double r, String cl, double  cw)
        {
            Centre = c;
            Radius = r;
            Colour = cl;
            circleWeight = cw;
        }
        /**
         * * End of myCircleClass
         * *---------------------------------------------------
         *
         */
    }
    /**
     * * End of myCircleClass
     * *---------------------------------------------------
     *
     */



    /**
     * ---------------------------------------------------------------------------------------------
     * myRangeDataClass
     *
     *  ClASS DESCRIPTION
     *      this is for the heat map it conrains the range and the colour used to create the
     *      heat map buckets
     *
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public static class myRangeDataClass
    {
        public double weightRange;
        public String Colour;

        /**
         * -----------------------------------------------------------------------------------------
         * myRangeDataClass(String c, double r)
         *
         *  FUNCTION DESCRIPTION
         *      This is the constructor
         *
         * @author Daniel Knight
         * @version 1.0
         * @since 30/07/2021
         * ---------------------------------------------------
         * @param c
         * @param r
         */
        public myRangeDataClass(String c, double r)
        {
            Colour = c;
            weightRange = r;
        }
        /**
         * * End of myRangeDataClass()
         * *---------------------------------------------------
         *
         */
    }
    /**
     * * End of myRangeDataClass
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * END OF SUBCLASSES
     * ---------------------------------------------------------------------------------------------
     */



    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION IS THE METHOD FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * onCreate(Bundle savedInstanceState)
     *
     *  FUNCTION DESCRIPTION
     *      First callback function to be run as per activity lifecycle
     *      Map fragment initialised
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        try {
            apiKey = getString(R.string.google_maps_key);
            SupportMapFragment myMapFragment;
            final GeoApiContext.Builder geoContextBuilder = new GeoApiContext.Builder()
                    .apiKey(apiKey);
            geoContext = geoContextBuilder.build();

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_maps);

            //Map fragment initialised
            myMapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.myMap, myMapFragment)
                    .commit();
            myMapFragment.getMapAsync(this);
        }
        catch(Exception e)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("initalisation error", "Critical error" + "line:" +
                    currentLine);
        }
    }
    /**
     * * End of onCreate
     * *---------------------------------------------------
     *
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * populateDecodedPathArray()
     *
     *  FUNCTION DESCRIPTION
     *      Firstly this function calls the DirectionsAPi with the google origin and destination.
     *      then it populates the decode route array for later processing
     *      This takes the path from the decoded path array from DirectionsAPI
     *      and extracts the route data
     *
     * SIDE EFFECTS
     *      adds to global variable: decodedPathArray
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public void populateDecodedPathArray()
    {
        //getting the route data from google (synchronous)
        try {
            final DirectionsApiRequest request = DirectionsApi.getDirections(geoContext,
                    routeOrigin, routeDestination);
            request.alternatives(true);
            myRoutes = request.await();
        } catch (Exception e) {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Routing Error",
                    "General Routing Error at line:" + currentLine);
            System.exit(ErrorCode);

        }

        //Initialization of the route arrays where Latlngs are going to be stored for plotting
        //The array is 2D one dimension for each route  returned and the other for each
        // point along the route.
        for (int i = 0; i < maxNumberofRoutes; i++)
        {
            decodedPathArray.add(new ArrayList());
        }
        int rc = 0; // route count

        //For each route returned access the leg, then the steps,
        // then the step data to the array
        for (DirectionsRoute r : myRoutes.routes)
        {
            for (DirectionsLeg l : r.legs)
            {
                for (DirectionsStep s : l.steps)
                {
                    List<com.google.maps.model.LatLng> slr;

                    slr = s.polyline.decodePath();

                    for (int i = 0; i < slr.size() - 1; i++)
                    {
                        LatLng ll = new LatLng(slr.get(i).lat, slr.get(i).lng);
                        if (rc < maxNumberofRoutes)
                        {
                            decodedPathArray.get(rc).add(ll);

                        } else {
                            //This can be overcome by the above
                            // dynamically(assuming a maximum of routes)
                            String currentLine = String.valueOf(getLineNumber());
                            messageBox("Routes Overflow",
                                        "Too many routes have been returned " +
                                                "At line:" + currentLine);
                            System.exit(ErrorCode);
                        }
                    }
                }
            }
            rc++;
            numberofRoutes = rc;
        }
    }
    /**
     * * End of populateDecodedPathArray
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * onActivityResult(int requestCode, int resultCode, Intent data)
     *
     *  FUNCTION DESCRIPTION
     *      This is a callback to allow for inter app communication.
     *      this is left in to enable greater future interaction in the apps
     *
     *      (Please see Documenentation)
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * End of onActivityResult
     * ----------------------------------------------------
     */



    /**
     * ---------------------------------------------------------------------------------------------
     * onMapReady(GoogleMap googleMap)
     *
     *  FUNCTION DESCRIPTION
     *      This callback allows the manipulation of the map fragment once application is ready.
     *      Will prompt the user to install Google play services, if not already done so.
     *
     *  SIDE EFFECTS
     *      Changes global variable:  mMap, routeRisk
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED)
            {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        //GoogleMapsOptions object
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(mMap.MAP_TYPE_TERRAIN)
                .compassEnabled(true)
                .rotateGesturesEnabled(true)
                .tiltGesturesEnabled(true);

        readExternalFile(); //Reads File in
        addHeatMap();//Current data display drawn as a heat map
        populateDecodedPathArray(); // Decodes Path
        if(drawPolylineRoute() != noError) //Draw route
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Polyline error",
                    "Undefined polyline error at line:" + currentLine);
            System.exit(ErrorCode);
        }
        routeRisk = new double[maxNumberofRoutes];
        for (int j = 0; j < numberofRoutes; j++) // number of routes
        {
            routeRisk[j] = calculateRouteRisk(j); //calculate Route Risk
        }

        addRiskLabels();

        //Camera initialisation and configuration of controls
        //moves to give a overview of selected route
        LatLng cameraPosll = new LatLng(myRoutes.routes[0].legs[0].startLocation.lat,
                                        myRoutes.routes[0].legs[0].startLocation.lng);
        CameraPosition cp = new CameraPosition.Builder()
                .target(cameraPosll)
                .zoom(13)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));

    }
    /**
     * End of onMapReady
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION ARE THE DRAWING FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * addRiskLabels()
     *
     *  FUNCTION DESCRIPTION
     *      This adds the Risk values onto a marker at the route origin,
     *      allowing the user to choose the lowest risk.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    private void addRiskLabels()
    {
        // This latlng is created since the latlng at "myRoutes.routes[0].legs[0].startLocation"
        // is not compatible with MarkerOptions
        LatLng ll = new LatLng(myRoutes.routes[0].legs[0].startLocation.lat,
                                myRoutes.routes[0].legs[0].startLocation.lng);
        String markerTitle = "Risks:";

        Marker riskMarker;

        for (int i = 0; i < numberofRoutes; i++)
        {
            markerTitle = markerTitle + colourName(routeColourArray[i]) +
                    ":" +
                    routeRisk[i] + "";
        }

        //Marker initialization and configuration
        MarkerOptions mo = new MarkerOptions()
                .position(ll)
                .title("Risks:")
                .snippet(markerTitle);
        riskMarker = mMap.addMarker(mo);
        riskMarker.showInfoWindow();
    }
    /**
     * * End of addRiskLabels()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * addHeatMap()
     *
     *  FUNCTION DESCRIPTION
     *      This creates a weighted version of LatLng that can be applied to create a heat map
     *      the weighted element is the sum count of severities in a given circle.
     *      This produces the intensities for the heat map
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    private void addHeatMap()
    {
        List<WeightedLatLng> wlatLngs = new ArrayList<>();

        //Creates Weighted LatLngs
        for(int i = 0; i <  MyCircles.size() - 1; i++)
        {
            LatLng ll = new LatLng(MyCircles.get(i).Centre.latitude,
                                    MyCircles.get(i).Centre.longitude);

            WeightedLatLng wll = new WeightedLatLng(ll,
                                                    MyCircles.get(i).circleWeight);
            wlatLngs.add(wll);
        }

    try {
        //heat map gradient is created with the colour array and the break points of the buckets
        Gradient grad = new Gradient(heatmapColours, heatmapStartPoint);

        // Create a heat map tile provider, passing it the latlngs of the dataset
        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .radius(40)
                .opacity(0.4)
                .weightedData(wlatLngs)
                .gradient(grad)
                .build();
    }
        catch(Exception e)
        {
            String currentLine = String.valueOf(getLineNumber());
            messageBox("Heatmap Error", e.toString() + " at Line" + currentLine);
            System.exit(ErrorCode);
        }
    }
    /**
     * * End of addHeatMap()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * drawPolylineRoute()
     *
     *  FUNCTION DESCRIPTION
     *      This draws the poly line route based on the Directions API information
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @return
     */
    private int drawPolylineRoute()
    {
        ArrayList<LatLng> temp; // temporary array list of latlngs used in this function only
        ArrayList<Polyline> plArray = new ArrayList<>(); // temp arraylist of polylines

        for (int i = 0; i < numberofRoutes; i++)
        {
            // sets up the polyline options for the current route
            PolylineOptions plo = new PolylineOptions();
            plo.color(routeColourArray[i]);
            plo.width(polyLineWidth);
            plo.clickable(true);

            // getting out the decoded latlng for ech route and putting tem into the plo array
            // taking an array of latlngs adding to plo putting plo into an array of polylines
            temp = decodedPathArray.get(i);
            for (int k = 0; k < temp.size(); k++) {
                LatLng ll = new LatLng(temp.get(k).latitude, temp.get(k).longitude);
                plo.add(ll);
            }
            //adding the plo array to the polyline
            Polyline p = mMap.addPolyline(plo);
            plArray.add(p);

            // seting up onclick listener for polyline
            mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
            {
                // this function checks the colour of the click line.
                //if yellow, returns
                // if red sets all line colours back to default and then returns
                //if an original colour sets to red
                // moves its position to the top of stack (zindex)
                // updates the route info bax (user directions info)
                // turns all non selected polylines yellow
                // sets the bounds to the boundingbox around a given route
                // then zooms the camera to the route.
                public void onPolylineClick(Polyline polyline)
                {
                    int c = polyline.getColor();
                    if(c == COLOR_YELLOW_ARGB)
                    {
                        return;
                    }
                    if(c == COLOR_RED_ARGB)
                    {
                        int i = 0;
                        for (Polyline p : plArray)
                        {
                            p.setColor(routeColourArray[i++]);
                            p.setZIndex(lowestZIndex);
                        }
                        return;
                    }
                    int r = getRouteNumber(c);
                    polyline.setColor(COLOR_RED_ARGB);
                    polyline.setZIndex(highestZIndex);
                    updateRouteInfoBox(r);
                    for (Polyline p : plArray)
                    {
                        if(p.getColor() != COLOR_RED_ARGB)
                        {
                            p.setColor(COLOR_YELLOW_ARGB);
                        }
                    }
                    LatLng llne = null;
                    LatLng llsw = null;
                    LatLngBounds llb = null;

                        llne = new LatLng(myRoutes.routes[r].bounds.northeast.lat,
                                            myRoutes.routes[r].bounds.northeast.lng);
                        llsw = new LatLng(myRoutes.routes[r].bounds.southwest.lat,
                                            myRoutes.routes[r].bounds.southwest.lng);
                        llb = new LatLngBounds(llsw, llne);

                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(llb, cameraZoom));
                }
            });
        }
        return noError;
    }
    /**
     * * End of drawPolylineRoute()
     * *---------------------------------------------------
     *
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * updateRouteInfoBox(int r)
     *
     *  FUNCTION DESCRIPTION
     *      Populates the screen display with the details of the route information
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param r
     */
    private void updateRouteInfoBox(int r)
    {
        EditText ed = findViewById(R.id.editText);
         DirectionsRoute rt = myRoutes.routes[r];

        String buffstring = "Directions to Destination: \n ";
        ed.setText(buffstring);
        buffstring = "";

            for (DirectionsLeg l : rt.legs)
            {
                for (DirectionsStep s : l.steps)
                {
                    //converts Html format of directions into comprehensible strings
                    buffstring = buffstring + Html.fromHtml(s.htmlInstructions).toString() + "\n";
                }
            }
        ed.setText(buffstring);
    }
    /**
     * * End of updateRouteInfoBox()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * calculateRouteRisk(int RT)
     *
     *  FUNCTION DESCRIPTION
     *      Calculates the route risk via checking for intersections of the route path points and
     *      the circle. Also uses the severity values to aid with the calculation.
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param RT
     * @return
     */
    public double calculateRouteRisk(int RT) {
        int i = 0; // counts the num of circles
        int j = 0; // counts the number of routes
        int k = 0;  // counts the number of LATLONG in route

        double totalRisk = 0.0;

            //Slight error could be increased by extending these by the circle radius at 45 degrees.
            LatLng x = new LatLng(myRoutes.routes[RT].bounds.southwest.lat,
                                myRoutes.routes[RT].bounds.southwest.lng);
            LatLng y = new LatLng(myRoutes.routes[RT].bounds.northeast.lat,
                                myRoutes.routes[RT].bounds.northeast.lng);

        // this is a temp variable to resolve issue with accessing size
            ArrayList<LatLng> temp = decodedPathArray.get(RT);

            //Stepping through the route in 'routeStepSize' value to reduce processing time
            for (k = 0; k < temp.size() - 1; k = k + routeStepSize)
            {

                LatLng rlt = new LatLng(decodedPathArray.get(RT).get(k).latitude,
                                        decodedPathArray.get(RT).get(k).longitude);

                for (i = 0; i < MyCircles.size() - 1; i++) {

                    int currentHit = 0; // gets reset on new circle

                    LatLng clt = MyCircles.get(i).Centre;
                    double r = MyCircles.get(i).Radius;

                    LatLngBounds llb = new LatLngBounds(x, y);

                    //check if the circle is inside the bounding box of the route (for optimization)
                    if (llb.contains(clt))
                    {// is in side -- carry on processing
                        //Check if route point is inside the circle.
                        if (isInsideCircle(clt, r, rlt, distanceUnits))
                        {
                            //Is inside Circle
                            //Stops there being multiple hits inside a circle
                            //takes it for the first hit
                            if (currentHit == 0) {
                                totalRisk = totalRisk + MyCircles.get(i).circleWeight;
                                currentHit++;
                            } else {
                                //if here in same circle
                                //Do nothing -- space holder to clarify what is going on
                            }
                        } else {
                            //
                            // Is not inside circle do nothing -- space holder to
                            // clarify what is going on
                        }
                    } // inside bounds -- space holder to clarify what is going on
                     else {
                         //if the circle center is not inside the bounding box of the route
                        //not inside bounds -- space holder to clarify what is going on
                    }
                }
            }
        return totalRisk;
    }
    /**
     * * End of calculateRouteRisk()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * isInsideCircle(LatLng c, Double r, LatLng l, String u)
     *
     *  FUNCTION DESCRIPTION
     *      Checks that the latlng point is inside the circle (centre c, radius r, of u Units)
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param c
     * @param r
     * @param l
     * @param u
     * @return
     */
    public boolean isInsideCircle(LatLng c, Double r, LatLng l, String u)
    {
        if(distanceBetween(c.latitude, c.longitude, l.latitude, l.longitude, u) < r) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * * End of isInsideCircle()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * distanceBetween(double lat1, double lon1, double lat2, double lon2, String unit)
     *
     *  See Below
     *
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::                                                                         :*/
    /*::  This routine calculates the distance between two points (given the     :*/
    /*::  latitude/longitude of those points). It is being used to calculate     :*/
    /*::  the distance between two locations using GeoDataSource (TM) products   :*/
    /*::                                                                         :*/
    /*::  Definitions:                                                           :*/
    /*::    Southern latitudes are negative, eastern longitudes are positive     :*/
    /*::                                                                         :*/
    /*::  Function parameters:                                                   :*/
    /*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
    /*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
    /*::    unit = the unit you desire for results                               :*/
    /*::           where: 'M' is statute miles (default)                         :*/
    /*::                  'K' is kilometers                                      :*/
    /*::                  'N' is nautical miles                                  :*/
    /*::  Worldwide cities and other features databases with latitude longitude  :*/
    /*::  are available at https://www.geodatasource.com                         :*/
    /*::                                                                         :*/
    /*::  For enquiries, please contact sales@geodatasource.com                  :*/
    /*::                                                                         :*/
    /*::  Official Web site: https://www.geodatasource.com                       :*/
    /*::                                                                         :*/
    /*::           GeoDataSource.com (C) All Rights Reserved 2019                :*/
    /*::                                                                         :*/
    /*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double distanceBetween(double lat1, double lon1, double lat2, double lon2, String unit)
    {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }
    /**
     * * End of distanceBetween()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * readExternalFile()
     *
     *  FUNCTION DESCRIPTION
     *         This reads the Externally saved file and parses and creates the necessary elements
     *         for display including the heat maps.
     *
     *  SIDE EFFECTS
     *          Changes global variables:
     *                      routeOrigin
     *                      routeDestination
     *                      circleSize
     *                      distanceUnits
     *                      HeatMapIncrements
     *                      rangeData
     *                      heatmapStartPoint
     *                      heatmapColours
     *
     *          Adds to global variable:
     *                      myRangeData
     *                      MyCircles
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    public void readExternalFile()
    {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (!Settings.System.canWrite(this))
                {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    if (isExternalStorageReadable())
                    {
                        try {
                            //Finds directory
                            File textFile = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                                    cdDataFile); //usable due to legacy mode enabled
                            FileInputStream fis = new FileInputStream(textFile);

                            if (fis != null) {
                                InputStreamReader isr = new InputStreamReader(fis);
                                BufferedReader buffread = new BufferedReader(isr);

                                String readline;
                                readline = buffread.readLine();
                                //get circle size in units and ranges
                                String[] wordtoken = readline.split(";");

                                //Parses first line datafile and assigns the variables
                                routeOrigin = wordtoken[0];
                                routeDestination = wordtoken[1];
                                circleSize = (Double.parseDouble(wordtoken[2]));
                                distanceUnits = wordtoken[3];
                                HeatMapIncrements = Integer.parseInt(wordtoken[4]);
                                rangeData = wordtoken[5];

                                // parses the range data at the end of the line
                                String[] wordtoken2 = rangeData.split(":");

                                // This reads maximum of heat map array to workout percentage
                                float maxHeatpointValue = Float.parseFloat(wordtoken2[(
                                        HeatMapIncrements * 2) - 1]);

                                // allocates the correct size of heat map for the data passed in
                                heatmapStartPoint = new float[HeatMapIncrements];
                                heatmapColours = new int[HeatMapIncrements];

                                // populates 2 above variables with correct data from data file
                                setUpHeatMapColours(heatmapColours, HeatMapIncrements);
                                for (int j = 0; j < (HeatMapIncrements * 2) - 1; j = j + 2) {

                                    //creates an rdc with the range, the first colour name.
                                    // the second one is the start point
                                    rdc = new myRangeDataClass(wordtoken2[j], Double.parseDouble(
                                            wordtoken2[j + 1]));
                                    //This calculates the average percentage values to go into
                                    // the heatmapStartPoints array (range of 0.0f - 1.0f)
                                    heatmapStartPoint[j / 2] = Float.parseFloat(
                                            wordtoken2[j + 1]) / maxHeatpointValue;

                                    // looks up the heat map integers from the heatmap colour array
                                    heatmapColours[j / 2] = getColourCode(wordtoken2[j]);
                                    myRangeData.add(rdc);
                                }
                                //Setting value R to radius
                                double R = circleSize;

                                //Parse the rest of the file, each line is in JSON format.
                                while ((readline = buffread.readLine()) != null) {
                                    //This is the layout of the data lines
                                    //{"lat" : 51.610976, "lng" : -0.26245, "wgt" : 2.0 },

                                    //if end of file "]" found stop reading
                                    if (readline.equals("]")) {
                                        break;
                                    }

                                    // parses the read line and retrieves the lat and long
                                    JSONObject j = new JSONObject(readline);
                                    double x = 0.0;
                                    double y = 0.0;
                                    x = j.getDouble("lat");
                                    y = j.getDouble("lng");

                                    //combines into a latLng
                                    LatLng ltlg = new LatLng(x, y);
                                    String cl = "PINK"; // initializes line colour to an error value
                                    double weight = j.getDouble("wgt"); // get line weight

                                    //for each map increment sets the colour of the circle
                                    //Although currently circles are not displayed
                                    //See documentation
                                    for (int K = 0; K < HeatMapIncrements; K++)
                                    {
                                        if (myRangeData.get(K).weightRange < weight) {
                                            cl = myRangeData.get(K).Colour;
                                        }
                                    }

                                    // creating allMyCircles (Center, radius, colour and weight)
                                    myCircleClass c = new myCircleClass(ltlg, R, cl,
                                            weight);
                                    MyCircles.add(c);
                                }
                                fis.close();
                            }
                        } catch (Exception e) {
                            String currentLine = String.valueOf(getLineNumber());
                            messageBox("Processing Error",
                                    e.getMessage() + " At line" + currentLine);
                            System.exit(ErrorCode);
                        }
                    }
                    else
                    {
                        String currentLine = String.valueOf(getLineNumber());
                        messageBox("File Error",
                                "File Space not Read/Writable - insufficient permissions "
                                        + "At line" + currentLine);
                        System.exit(ErrorCode);
                    }
            }
        }
    }
    /**
     * End of readExternalFile()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * setUpHeatMapColours(int[] hmc, int hmi)
     *
     *  FUNCTION DESCRIPTION
     *      This populates the heat map colour array dynamically according
     *      to the amount of data available if less than 7(hmi) -- (design decision)
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param hmc
     * @param hmi
     */
    public void setUpHeatMapColours(int[] hmc, int hmi)
    {
        switch(hmi - 1)
        {
            case 1:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_WHITE_ARGB;
                break;
            case 2:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_ORANGE_ARGB;
                hmc[2] = COLOR_WHITE_ARGB;
                break;
            case 3:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_BLUE_ARGB;
                hmc[2] = COLOR_ORANGE_ARGB;
                hmc[3] = COLOR_WHITE_ARGB;
                break;
            case 4:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_BLUE_ARGB;
                hmc[2] = COLOR_ORANGE_ARGB;
                hmc[3] = COLOR_YELLOW_ARGB;
                hmc[4] = COLOR_WHITE_ARGB;
                break;
            case 5:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_BLUE_ARGB;
                hmc[2] = COLOR_GREEN_ARGB;
                hmc[3] = COLOR_ORANGE_ARGB;
                hmc[4] = COLOR_YELLOW_ARGB;
                hmc[5] = COLOR_WHITE_ARGB;
                break;
            case 6:
                hmc[0] = COLOR_BLACK_ARGB;
                hmc[1] = COLOR_BLUE_ARGB;
                hmc[2] = COLOR_GREEN_ARGB;
                hmc[3] = COLOR_ORANGE_ARGB;
                hmc[4] = COLOR_RED_ARGB;
                hmc[5] = COLOR_YELLOW_ARGB;
                hmc[6] = COLOR_WHITE_ARGB;
                break;
        }
    }
    /**
     * * End of setUpHeatMapColours
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * isExternalStorageReadable()
     *
     *  FUNCTION DESCRIPTION
     *      This checks the devices permissions
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @return
     */
    private boolean isExternalStorageReadable()
    {   ///Checks that external storage is able to be at least read
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    /**
     * End of isExternalStorageReadable()
     * ----------------------------------------------------
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * buildGoogleApiClient()
     *
     *  FUNCTION DESCRIPTION
     *      configuring the client and connecting to the Google API servers
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     */
    protected synchronized void buildGoogleApiClient()
    {
        // TO DO - Depreciated code needs to updated
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    /**
     * End of isExternalStorageReadable()
     * ----------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * onLocationChanged(Location location)
     *
     *  FUNCTION DESCRIPTION
     *      Dummy function for callback functions
     *
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) { }
    /**
     * End of onLocationChanged()
     * ----------------------------------------------------
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * THESE ARE THE CONVERSION FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * int getColourCode(String s)
     *
     *  FUNCTION DESCRIPTION
     *      This convert the colour name to the colour code
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param s
     * @return
     */
    public int getColourCode(String s)
    {
        switch (s)
        {
            case "Black":
                return COLOR_BLACK_ARGB;

            case "Blue":
                return COLOR_BLUE_ARGB;

            case "Green":
                return COLOR_GREEN_ARGB;

            case "Orange":
                return COLOR_ORANGE_ARGB;

            case "Red":
                return COLOR_RED_ARGB;

            case "Yellow":
                return COLOR_YELLOW_ARGB;

            case "White":
                return COLOR_WHITE_ARGB;

            default:
                return COLOR_PINK_ARGB;
        }
    }
    /**
     * End of getColourCode
     * ----------------------------------------------------
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * String colourName(int x)
     *
     *  FUNCTION DESCRIPTION
     *      This converts the colour code to a name (String)
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param x
     * @return
     */
    private String colourName(int x)
    {
        switch (x)
        {
            case COLOR_BLACK_ARGB:
                return "Black";

            case COLOR_BLUE_ARGB :
                return "Blue";

            case COLOR_GREEN_ARGB :
                return "Green" ;

            case COLOR_ORANGE_ARGB:
                return "Orange";

            case COLOR_RED_ARGB:
                return "Red" ;

            case COLOR_YELLOW_ARGB:
                return "Yellow";

            case COLOR_WHITE_ARGB:
                return "White";

            default:
                return "Unknown";
        }
    }
    /**
     * * End of colourName
     * *---------------------------------------------------
     *
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * int getRouteNumber(int Colour)
     *
     *  FUNCTION DESCRIPTION
     *      this gets the route number based on the colour
     *      route number is the location in the route array
     *
     * @author Daniel Knight
     * @version 1.0
     * @since 30/07/2021
     * ---------------------------------------------------
     * @param Colour
     * @return
     */
    private int getRouteNumber(int Colour)
    {
        switch (Colour)
        {
            case COLOR_BLACK_ARGB:
                return 0;

            case COLOR_BLUE_ARGB:
                return 1;

            case COLOR_GREEN_ARGB:
                return 2;

            case COLOR_ORANGE_ARGB:
                return 3;

            case COLOR_PURPLE_ARGB:
                return 4;

            default:
                return -1;
        }
    }
    /**
     * * End of getRouteNumber()
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION ARE THE EMPTY OVERRIDE FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }
    @Override
    public void onProviderEnabled(String s) { }
    @Override
    public void onProviderDisabled(String s) { }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }
    @Override
    public void onConnected(Bundle bundle) { }
    @Override
    public void onConnectionSuspended(int i) { }

    /**
     * * End of Empty Override
     * *---------------------------------------------------
     *
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * THIS SECTION ARE THE HELPER FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * ---------------------------------------------------------------------------------------------
     * messageBox(String method, String message)
     *
     *  FUNCTION DESCRIPTION
     *      generic dialog, takes in the method name and error message
     *
     * Reference:
     * https://stackoverflow.com/questions/16561692/android-exception-handling-best-practice
     * @version 1.0
     * Accessed: 30/07/2021
     * ---------------------------------------------------
     * @param method
     * @param message
     */
    private void messageBox(String method, String message)
    {
        Log.d("EXCEPTION: " + method,  message);

        AlertDialog.Builder messageBox = new AlertDialog.Builder(this);
        messageBox.setTitle(method);
        messageBox.setMessage(message);
        messageBox.setCancelable(false);
        messageBox.setNeutralButton("Ok", null);
        messageBox.show();
    }
    /**
     * * End of 'messageBox()'
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * getLineNumber()
     *
     *  FUNCTION DESCRIPTION
     *      accompanying function to ___8drrd3148796d_Xaf()
     *      Used for debugging purposes to return the line number for error messages
     *
     * @author Brian_Entei
     * @version 1.0
     * @since 1/08/2021
     * Accessed: 30/07/2021
     * Reference:
     * https://stackoverflow.com/questions/17473148/dynamically-get-the-current-line-number%EF%BC%89
     * ---------------------------------------------------
     * @return The line number of the code that ran this method
     * */
    public static int getLineNumber() {
        return ___8drrd3148796d_Xaf();
    }
    /**
     * * End of getLineNumber
     * *---------------------------------------------------
     *
     */


    /**
     * ---------------------------------------------------------------------------------------------
     * ___8drrd3148796d_Xaf()
     *
     *  FUNCTION DESCRIPTION
     *      This methods name is ridiculous on purpose to prevent any other method
     *      names in the stack trace from potentially matching this one.
     *      (See reference for more detail)
     *
     * @author Brian_Entei
     * @version 1.0
     * @since 1/08/2021
     * Accessed: 30/07/2021
     * Reference:
     * https://stackoverflow.com/questions/17473148/dynamically-get-the-current-line-number%EF%BC%89
     * ---------------------------------------------------
     * @return
     */
    private static int ___8drrd3148796d_Xaf() {
        boolean thisOne = false;
        int thisOneCountDown = 1;
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for(StackTraceElement element : elements) {
            String methodName = element.getMethodName();
            int lineNum = element.getLineNumber();
            if(thisOne && (thisOneCountDown == 0)) {
                return lineNum;
            } else if(thisOne) {
                thisOneCountDown--;
            }
            if(methodName.equals("___8drrd3148796d_Xaf")) {
                thisOne = true;
            }
        }
        return -1;
    }
    /**
     * * End of ___8drrd3148796d_Xaf()
     * *---------------------------------------------------
     *
     */
    /**
     * ---------------------------------------------------------------------------------------------
     * END OF THE HELPER FUNCTIONS
     * ---------------------------------------------------------------------------------------------
     */
}

/**
 * ---------------------------------------------------------------------------------------------
 * END OF CLASS
 * ---------------------------------------------------------------------------------------------
 */



