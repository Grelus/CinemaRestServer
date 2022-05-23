import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;


public class DBConnector {
	
	private Properties properties = new Properties();
	
	public DBConnector(String user,String password) {
		properties.setProperty("user", user);
		properties.setProperty("password", password);
		//properties.setProperty("characterEncoding", "UTF-8");
		//properties.setProperty("useUnicode", "true");
	}
	
	private ResultSet queryDB(String query) throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306",properties); //only meant to be run locally
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery(query);
		s.close();
		connection.close();
		return rs;
	}
	
	//change methods to class and include in map 
	/*
	 * string query
	 * string[] arguments
	 */
	
	public ResultSet getScreenings(String from,String to) throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306",properties); //only meant to be run locally
		Statement s = connection.createStatement();
		String query = "select screening.id,screening.starting_time,movie.name "
				+ "from cinema_db.screening "
				+ "join cinema_db.movie on screening.movie_id = movie.id "
				+ "where starting_time > '%s' and starting_time < '%s' "
				+ "order by starting_time asc, name asc;";
		query  = String.format(query,from,to);
		return s.executeQuery(query);
	}
	
	public ResultSet getScreeningInformation(int screening_id) throws SQLException {
		String query = "select screening.id, screening.starting_time, movie.name, movie.duration, room.number, room.row_quantity, room.row_length "
				+ "from cinema_db.screening "
				+ "join cinema_db.movie on screening.movie_id = movie.id "
				+ "join cinema_db.room on screening.room_id = room.id "
				+ "where screening.id = %s;";
		query  = String.format(query,""+screening_id);
		return queryDB(query);
	}
	
	public ResultSet getReservations(int screening_id) throws SQLException {
		String query = "select reservation.seat_no "
				+ "from cinema_db.reservation "
				+ "where screening_id = %s;";
		query = String.format(query, ""+screening_id);
		return queryDB(query);
	}
	
	public ResultSet getReservation(int reservation_id) throws SQLException {
		String query = "select * "
				+ "from cinema_db.reservation_info "
				+ "where id = %s;";
		query = String.format(query, "" + reservation_id);
		return queryDB(query);
	}
	
	public Integer getLastInsertedInfo() throws SQLException {
		String query = "SELECT id from cinema_db.reservation_info order by id desc;";
		ResultSet rs = queryDB(query);
		if(rs.next())
			return rs.getInt("id");
		return null;
	}
	
	public ResultSet insertReservatonInfo(String name, String surname,String expiration) throws SQLException {
		String query = "insert into "
				+ "cinema_db.reservation_info(name,surname,expiration) "
				+ "values( '%s' , '%s' , '%s');";
		query = String.format(query, name,surname,expiration);
		return queryDB(query);
	}
	
	public ResultSet insertSeatReservation(int screening_id, int reservation_info_id,int seat_no,String type) throws SQLException {
		String query = "insert into "
				+ "cinema_db.reservation(seat_no,type,reservation_info_id,screening_id) "
				+ "values( %s , '%s' , %s , %s );";
		query = String.format(query, seat_no,type,reservation_info_id,screening_id);
		return queryDB(query);
	}
	
	public static JSONObject getJsonRow(ResultSet rs) throws JSONException, SQLException, UnsupportedEncodingException {
		JSONObject jo = new JSONObject();
		
		for(int i=1; i<=rs.getMetaData().getColumnCount(); i++)
			if(rs.getMetaData().getColumnClassName(i).equals(String.class.getName())) {
				String s = new String(rs.getBytes(i),"utf8"); 
				jo.append(rs.getMetaData().getColumnName(i), s);
			}
			else {
				jo.append(rs.getMetaData().getColumnName(i), rs.getObject(i));
			}
		return jo;
	}
}
