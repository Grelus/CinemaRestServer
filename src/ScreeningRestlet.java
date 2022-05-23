import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;

public class ScreeningRestlet extends Restlet{
	private DBConnector dbc;
	
	public ScreeningRestlet(DBConnector dbc) {
		this.dbc = dbc;
	}
	
	public void handle(Request request,Response response) {
		int screening_id = Integer.parseInt((String)request.getAttributes().get("id")); //null check?? 
		String host = request.getHostRef().getHostDomain() + ":" + request.getHostRef().getHostPort();
		//add all this to individual methods
		if(request.getMethod() == Method.POST) { //structure ?
			String body = request.getEntityAsText();
			// name,surname,(seat no),(seat type),(seat no)...
			String[] arguments = splitArguments(body);
			if(arguments.length < 4|| arguments.length%2 != 0 || arguments[1].length()>32 || arguments[0].length() > 32 ) {
				response.setEntity("wrong request content",MediaType.TEXT_HTML);
				return;
			}
			
			if(!Character.isUpperCase(arguments[0].charAt(0)) || !Character.isUpperCase(arguments[1].charAt(0))) {
				response.setEntity("names have to begin with capital letter",MediaType.TEXT_HTML);
				return;
			}
			ArrayList<Integer> seat_no = new ArrayList<>();
			ArrayList<String> seat_t = new ArrayList<>();
			for(int i = 2; i< arguments.length;i++) {
				if(i%2 == 1) {
					String type = arguments[i].substring(0, 1);
						if(type.equals("C") && type.equals("D") && type.equals("S")) {
							response.setEntity("wrong request content, problem with seat data",MediaType.TEXT_HTML);
							return;
						}
					seat_t.add(type);
				}
				else
					try {
						seat_no.add(Integer.parseInt(arguments[i]));
					}catch(java.lang.NumberFormatException ex) {
						response.setEntity("wrong request content, problem with seat data",MediaType.TEXT_HTML);
						return;
					}
			}
			try {
				String message = makeReservations(screening_id,arguments[0],arguments[1],seat_no,seat_t);
				response.setEntity(message,MediaType.TEXT_HTML);
			} catch (JSONException | UnsupportedEncodingException | SQLException e) {
				response.setEntity("problem while performing querry",MediaType.TEXT_HTML);
				e.printStackTrace();
			}catch(Exception e) {
				response.setEntity("too late to reserve seats for that movie now",MediaType.TEXT_HTML);
				e.printStackTrace();
			}
		}
		else if(request.getMethod() == Method.GET) {
			try {
				String message  = provideScreeningInfo(screening_id,host);
				if(message == null) {
					response.setEntity("no results",MediaType.TEXT_HTML);
					return;
				}
				response.setEntity(message ,MediaType.TEXT_HTML);
			} catch (SQLException | JSONException | UnsupportedEncodingException e) {
				e.printStackTrace();
				response.setEntity("problem while performing querry",MediaType.TEXT_HTML);
			}
		}
	}
	
	public String[] splitArguments(String body) { // might check contents of the body here, not in handle
		String[] split = body.split(";");
		return split;
	}
	
	private String provideScreeningInfo(int screening_id,String host) throws SQLException, JSONException, UnsupportedEncodingException {
		//probably should shorten and break into smaller methods
		
		JSONObject jo = new JSONObject();
		ResultSet rs = dbc.getScreeningInformation(screening_id);
		if(rs.next())
			jo =  DBConnector.getJsonRow(rs);
		else {
			return null;
		}
		int length = rs.getInt("row_length");
		int quantity = rs.getInt("row_quantity");
		ArrayList<Integer> seats = new ArrayList<Integer>();
		for(int n =0;n<quantity;n++)
			for(int m=0;m<length;m++)
				seats.add(m+n*length);
		rs = dbc.getReservations(screening_id);
		while(rs.next())
			if(seats.contains(rs.getInt("seat_no")))
				seats.remove(rs.getInt("seat_no"));
		jo.append("free seats", seats);
		addLinks(jo,host,screening_id);
		return jo.toString();
	}
	
	private String makeReservations(int screening_id,String name, String surname,ArrayList<Integer> seats_no, ArrayList<String> seats_t) throws Exception {
		//probably should make these 3 db queries work on one db connection, check for operations on reserved seats,
		ResultSet rs = dbc.getScreeningInformation(screening_id);
		Date now= new Date();
		Date screening = new Date();
		if(rs.next()) 
			screening =rs.getDate("starting_time");
		else
			throw new Exception("problem with retrieving screening date");
		long diff = screening.getTime() - now.getTime();
		if(diff < 15*60*1000)
			throw new Exception("too late to register");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date expiration = new Date(0);
		expiration.setTime(screening.getTime() - 30*60*1000);
		dbc.insertReservatonInfo(name, surname,sdf.format(expiration));
		int last = dbc.getLastInsertedInfo();
		float total = 0.0f;
		for(int i = 0; i<seats_t.size();i++)
		{
			dbc.insertSeatReservation(screening_id, last, seats_no.get(i), seats_t.get(i));
			if(seats_t.get(i).equals("D"))
				total+=25.0f;
			else if(seats_t.get(i).equals("S"))
				total+=18.0f;
			else if(seats_t.get(i).equals("C"))
				total+=12.5f;
		}
		JSONObject jo = new JSONObject();
		jo.append("name", name);
		jo.append("surname", surname);
		jo.append("expiration", sdf.format(expiration));
		jo.append("total ", total);
		return jo.toString();
	}
	
	public JSONObject addLinks(JSONObject state,String host, int screening_id) {
		JSONObject tmp = new JSONObject();
		String screening = host + "/screening/%s";
		screening = String.format(screening, screening_id);
		tmp.append("self", screening);
		tmp.append("(post)reserve seats", screening);
		state.append("links", tmp);
		return state;
	}

}
