package me.dbarnett.acastus;

/**
 * Created by daniel on 7/24/16.
 */
public class ResultNode implements Comparable< ResultNode >{

    public String name;
    public double lat;
    public double lon;
    public double distance;

    @Override
    public int compareTo(ResultNode o) {
        return (int) Math.round(this.distance - o.distance);
    }

    @Override
    public String toString() { return name; }
}
