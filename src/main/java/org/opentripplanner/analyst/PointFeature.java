package org.opentripplanner.analyst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.opentripplanner.analyst.PointSet.AttributeData;

import com.bedatadriven.geojson.GeometryDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PointFeature implements Serializable {

	private static final long serialVersionUID = -613136927314702334L;
	
	private String id;
	private Geometry geom;
	private List<AttributeData> attributes;
	private double lat;
	private double lon;
	
	public PointFeature(){
		// blank constructor for deserialization
		this(null);
	}
	
	public PointFeature(String id){
		this.id = id;
		this.geom = null;
		this.attributes = new ArrayList<AttributeData>();
	}
	
	public PointFeature(String id, Geometry g,  ArrayList<AttributeData> ad) throws EmptyPolygonException, UnsupportedGeometryException{
		this.id = id;
		this.setGeom(g);
		this.attributes = ad;
	}
	
	public void addAttribute( AttributeData data ){
		this.attributes.add( data );
	}

	public void setGeom(Geometry geom) throws EmptyPolygonException, UnsupportedGeometryException {
		if (geom instanceof MultiPolygon) {
			if (geom.isEmpty()) {
				throw new EmptyPolygonException();
			}
			if (geom.getNumGeometries() > 1) {
				// LOG.warn("Multiple polygons in MultiPolygon, using only the first.");
				// TODO percolate this warning up somehow
			}
			this.geom = geom.getGeometryN(0);
		} else if( geom instanceof Point || geom instanceof Polygon){
			this.geom = geom;
		} else {
			throw new UnsupportedGeometryException( "Non-point, non-polygon Geometry, not supported." );
		}
		
		// cache a representative point
		Point point = geom.getCentroid();
		this.lat = point.getY();
		this.lon = point.getX();
	}
	
	public Polygon getPolygon(){
		if( geom instanceof Polygon ){
			return (Polygon)geom;
		} else {
			return null;
		}
	}

	public Geometry getGeom() {
		return geom;
	}

	public List<AttributeData> getAttributes() {
		return attributes;
	}

	public String getId() {
		return id;
	}

	public static PointFeature fromJsonNode(JsonNode feature) throws EmptyPolygonException, UnsupportedGeometryException {
		if (feature.getNodeType() != JsonNodeType.OBJECT)
			return null;
		JsonNode type = feature.get("type");
		if (type == null || !type.asText().equalsIgnoreCase("Feature"))
			return null;
		JsonNode props = feature.get("properties");
		if (props == null || props.getNodeType() != JsonNodeType.OBJECT)
			return null;
		JsonNode structured = props.get("structured");
		List<AttributeData> attributes = Lists.newArrayList();
		if (structured != null && structured.getNodeType() == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> catIter = structured.fields();
			while (catIter.hasNext()) {
				Entry<String, JsonNode> catEntry = catIter.next();
				String catName = catEntry.getKey();
				JsonNode catNode = catEntry.getValue();
				Iterator<Entry<String, JsonNode>> attrIter = catNode.fields();
				while (attrIter.hasNext()) {
					Entry<String, JsonNode> attrEntry = attrIter.next();
					String attrName = attrEntry.getKey();
					int magnitude = attrEntry.getValue().asInt();
					// TODO Maybe we should be using a String[2] instead of
					// joined strings.
					attributes.add(new AttributeData(catName, attrName, magnitude));
				}
			}
		}

		String id = null;
		JsonNode idNode = feature.get("id");
		if (idNode != null)
			id = idNode.asText();

		Geometry jtsGeom = null;
		JsonNode geom = feature.get("geometry");
		if (geom != null && geom.getNodeType() == JsonNodeType.OBJECT) {
			GeometryDeserializer deserializer = new GeometryDeserializer(); // FIXME
																			// lots
																			// of
																			// short-lived
																			// objects...
			jtsGeom = deserializer.parseGeometry(geom);
		}

		PointFeature ret = new PointFeature(id);
		ret.setGeom(jtsGeom);
		ret.setAttributes(attributes);

		return ret;
	}

	private void setAttributes(List<AttributeData> attributes) {
		this.attributes = attributes;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getLat() {
		return this.lat;
	}
	
	public double getLon() {
		return this.lon;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

}