
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.map.MarkerDragEvent;
import org.primefaces.event.map.OverlaySelectEvent;
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
    private Marker markerCurresnt;
    private String status=  "";


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Marker getMarkerCurresnt() {
        return markerCurresnt;
    }

    public void setMarkerCurresnt(Marker markerCurresnt) {
        this.markerCurresnt = markerCurresnt;
    }
    @PostConstruct
    public void init() {
        emptyModel = new DefaultMapModel();
        IniciarPuntos();
    }
    public void IniciarPuntos() {
        leerDB();
        for(Marker premarker : emptyModel.getMarkers()) {
            premarker.setDraggable(true);
           
        }
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
    public void onMarkerSelect(OverlaySelectEvent event) {
        markerCurresnt = (Marker) event.getOverlay();
        Marker  mkF = Find( ((Marker) event.getOverlay()).getId()) ; 
        markerCurresnt =   mkF ;
        setStatus(String.valueOf(new Random().nextInt()));
    }
    private int insertRegistros(Connection pcnx, String sqlStatement, Marker pLatLng) {
        PreparedStatement psta = null;
        ResultSet keyset = null;
        int currentInsert = -1 ; 
        try {
            psta = pcnx.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS);
            psta.setString(1, pLatLng.getTitle());
            psta.setObject(2, pLatLng.getLatlng().getLng());
            psta.setObject(3, pLatLng.getLatlng().getLat());
            psta.executeUpdate();           
            keyset = psta.getGeneratedKeys();
            while (keyset.next()) {
                currentInsert = keyset.getInt("puntos_id");
            }
            psta.close();
        } catch (SQLException ex) {
            GenereraError("Error", ex.getMessage());
            Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                keyset.close();
            } catch (SQLException ex) {
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        return currentInsert  ;
    }

    
     public Marker Find(String  Key){
        for (Marker  x  :  emptyModel.getMarkers()){
            if (x.getId() == null ? Key == null : x.getId().equals(Key)){
            return x ;
            }  
        }
        return null ;
     }    
   public void onMarkerDrag(MarkerDragEvent event) {
        Marker  mkF = Find(event.getMarker().getId()) ; 
        cambiarDB(mkF, event.getMarker()); 
        markerCurresnt =   mkF ;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto cambiado", "Lat:" + markerCurresnt.getLatlng().getLat() + ", Lng:" + markerCurresnt.getLatlng().getLng()));
    }
    public int guardarDB(Marker mk) {
        final Connection cnx;
        int   keyID =  -1 ;
        try {
            cnx = BaseConexion.getConectar();
            String insertPointSQL = "INSERT INTO puntos  (nombre , punto_map) values( ? , ST_SetSRID(ST_MakePoint(?, ?), 4326))";
            keyID =insertRegistros(cnx, insertPointSQL, mk);
        } finally {
            try {
                BaseConexion.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return  keyID ;
        
    }

    public void leerDB() {
        final Connection cnx;
        Statement sentencia = null;
        ResultSet rs = null;
        try {
            cnx = BaseConexion.getConectar();
            String selectPointSQL = "select  puntos_id ,  nombre  ,descripcion , ST_y(punto_map)  as Lat , ST_X(punto_map) as Lng   from   puntos   ";
            sentencia = cnx.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = sentencia.executeQuery(selectPointSQL);

            while (rs.next()) {
                MarkerData x = new MarkerData(rs.getInt("puntos_id"), rs.getString("descripcion"));
                Marker puntom = new Marker(new LatLng(rs.getDouble("lat"), rs.getDouble("lng")), rs.getString("nombre"),x);
                puntom.setDraggable(true);
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
        marker.setDraggable(true);
        emptyModel.addOverlay(marker); 
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto Agregado", "Lat:" + lat + ", Lng:" + lng));
        guardarDB(marker);
        
        
        
        MarkerData mData = new MarkerData(-1, "---");
        Marker mInsert = new Marker(new LatLng(lat, lng), title, mData);
        int  RowId=   guardarDB(mInsert);
        mData.setPoint_ID(RowId);
        emptyModel.addOverlay(mInsert);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto Agregado", "Lat:" + lat + ", Lng:" + lng));
        setTitle("");
        setLat(0);
        setLng(0);
        
    }
    
    
    public   void removeMarket(ActionEvent actionEvent){
    removeMarket();
    
    }
   public   void removeMarket(){
      elimiarDB(markerCurresnt);
      emptyModel = new DefaultMapModel();
      leerDB();
    }
    
      public void cambiarDB(Marker mkNew  ,Marker mkOld  ) {
        final Connection cnx;
        PreparedStatement  stm = null;
        try {
            cnx = BaseConexion.getConectar();
            String deletePointSQL = "update puntos  set  punto_map  =  ST_SetSRID(ST_MakePoint(?, ?), 4326)   where  puntos_id  =  ? " ;     
            stm = cnx.prepareStatement(deletePointSQL);  
            stm.setObject(1, mkNew.getLatlng().getLng());
            stm.setObject(2, mkNew.getLatlng().getLat());
            stm.setInt(3, ((MarkerData)(mkOld.getData())).getPoint_ID());
            stm.executeUpdate();
        }  catch (SQLException ex) {
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stm.close();
                BaseConexion.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());

            }
        }
    }
   
   public void elimiarDB(Marker mk) {
        final Connection cnx;
        Statement  stm = null;
        try {
            cnx = BaseConexion.getConectar();
            String deletePointSQL = "delete from puntos  where puntos_id  =  "+((MarkerData) (mk.getData())).getPoint_ID() ;     
            stm = cnx.createStatement();
            //stm.executeUpdate(deletePointSQL) ;
        }  catch (SQLException ex) {
                Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stm.close();
                BaseConexion.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());

            }
        }
    }
 
}
