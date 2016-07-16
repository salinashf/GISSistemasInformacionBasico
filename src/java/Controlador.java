
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;
import utilidades.conexion.BaseConexion;


@ManagedBean(name = "addMarkersView")
@SessionScoped
public class Controlador implements Serializable {

    private MapModel emptyModel;
    private String title;
    private double lat;
    private double lng;

    @PostConstruct
    public void init() {
        emptyModel = new DefaultMapModel();
        IniciarPuntos();
    }
    public void IniciarPuntos() {
        leerDB();
    }

    public MapModel getEmptyModel() {
        return emptyModel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    private void insertRegistros(Connection pcnx, String sqlStatement, Marker pLatLng) {
        PreparedStatement psta = null;
        try {
            psta = pcnx.prepareStatement(sqlStatement);
            psta.setString(1, pLatLng.getTitle());
            psta.setObject(2, pLatLng.getLatlng().getLng());
            psta.setObject(3, pLatLng.getLatlng().getLat());
            psta.executeUpdate();
            psta.close();
        } catch (SQLException ex) {
            GenereraError("Error", ex.getMessage());
            Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (psta != null) {
                try {
                    if (!psta.isClosed()) {
                        psta.close();
                    }
                } catch (SQLException ex) {
                    GenereraError("Error", ex.getMessage());
                    Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void guardarDB(Marker mk) {
        final Connection cnx;
        try {
            cnx = BaseConexion.getConectar();
            String insertPointSQL = "INSERT INTO puntos  (nombre , punto_map) values( ? , ST_SetSRID(ST_MakePoint(?, ?), 4326))";
            insertRegistros(cnx, insertPointSQL, mk);
        } finally {
            try {
                BaseConexion.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void leerDB() {
        final Connection cnx;
        Statement sentencia = null;
        ResultSet rs = null;
        try {
            cnx = BaseConexion.getConectar();
            String selectPointSQL = "select    nombre  , ST_y(punto_map)  as Lat , ST_X(punto_map) as Lng   from   puntos   ";
            sentencia = cnx.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = sentencia.executeQuery(selectPointSQL);

            while (rs.next()) {
                Marker puntom = new Marker(new LatLng(rs.getDouble("lat"), rs.getDouble("lng")), rs.getString("nombre"));
                
                emptyModel.addOverlay(puntom);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sentencia != null) {
                    sentencia.close();
                }
                BaseConexion.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void GenereraError(String vmsmTitle, String vmsgContent) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, vmsmTitle, vmsgContent));
    }
    public void addMarker() {
        Marker marker = new Marker(new LatLng(lat, lng), title);        
        emptyModel.addOverlay(marker);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto Agregado", "Lat:" + lat + ", Lng:" + lng));
        guardarDB(marker);
        setTitle("");
        setLat(0);
        setLng(0);
    }
 
}
