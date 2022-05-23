import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;

public class ScheduleRestlet extends Restlet{
	private DBConnector dbc;
	
	public ScheduleRestlet(DBConnector connector) {
		this.dbc = connector;
	}
	
	public void handle(Request request,Response response) {
		String message = "response";
		Form f = request.getResourceRef().getQueryAsForm();
		String[] query_args = {"to","from"}; 
		String host = request.getResourceRef().getHostDomain()+":"+request.getResourceRef().getHostPort();
		
		if(f.getNames().containsAll(Arrays.asList(query_args))) {
			String[] dates = checkDates(f.getFirstValue("from"),f.getFirstValue("to"));
			
			if(dates == null )
				message =  "wrong querry argument/s";
			else{
					try {
						message = "requested schedule between " + dates[0]+"|"+dates[1];
						message = message + "result:" + provideSchedule(dates,host);
						
					} catch (JSONException | UnsupportedEncodingException | SQLException e) {
						e.printStackTrace();
						message = "encountered error during querry";
					}
			}
		}
		else
			message = "incorrect URI, incomplete querry";
		response.setEntity(message,MediaType.TEXT_HTML);
	}
	
	private String[] checkDates(String from,String to) {
		SimpleDateFormat sdf_in = new SimpleDateFormat("yyyy-MM-dd'@'hh:mm:ss");
		SimpleDateFormat sdf_out = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		try {
			Date d_from = sdf_in.parse(from);
			Date d_to = sdf_in.parse(to);
			if(d_to.before(d_from))
				return null;
			else {
				String[] s = {sdf_out.format(d_from),sdf_out.format(d_to)};
				return s;
			}
		} catch (ParseException e) {
			return null;
		}
	}
	
	private String provideSchedule(String[] dates,String host) throws SQLException, JSONException, UnsupportedEncodingException {
		ResultSet rs = dbc.getScreenings(dates[0], dates[1]);
		JSONObject jo = new JSONObject();
		int count = 0;
		while(rs.next()) {
			JSONObject tmp = DBConnector.getJsonRow(rs);
			jo.append(count+"",addLinks(DBConnector.getJsonRow(rs),host,rs.getInt("screening.id")+""));
			
		}
		byte[] b = jo.toString().getBytes("UTF8");
		return jo.toString();
	}
	
	private ArrayList<String> listStrings(String host,int id){
		return null;
	}
	
	private JSONObject addLinks(JSONObject state,String host,String screening_id) {
		JSONObject tmp = new JSONObject();
		tmp.append("self", host + "/schedule?from&to");
		String screening = host + "/screening/%s";
		screening = String.format(screening, screening_id);
		tmp.append("more infromation about screening", screening);
		state.append("links", tmp);
		return state;
	}
}
