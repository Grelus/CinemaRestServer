
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.routing.VirtualHost;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		DBConnector mysqlcon = new DBConnector("tester","pass");
		
		Component component = new Component();
		component.getServers().add(Protocol.HTTP,8080);
		
		VirtualHost host= component.getDefaultHost();
		
		ScheduleRestlet schedule = new ScheduleRestlet(mysqlcon);
		ScreeningRestlet screening = new ScreeningRestlet(mysqlcon);
		
		
		host.attach("/schedule",schedule);
		host.attach("/screening/{id}",screening);
		
		component.start();
		
	}

}
