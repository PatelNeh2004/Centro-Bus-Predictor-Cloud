package edu.oswego.csc380.centrobus.data;

import com.google.gson.annotations.SerializedName;

//Used for handling JSON data
public class Root {

    //need to use SerializedName due to Java syntax forbidding the use of '-' in variable names
    @SerializedName("bustime-response")
    public BustimeResponse bustimeResponse;

}
