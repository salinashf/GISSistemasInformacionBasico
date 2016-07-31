
import java.io.Serializable;
public class MarkerData implements Serializable{

    private String description;
    private int point_ID =0;

    public int getPoint_ID() {
        return point_ID;
    }

    public void setPoint_ID(int point_ID) {
        this.point_ID = point_ID;
        
    }
    

     MarkerData(int _pointID , String _descripcion) {
        this.description = _descripcion ;
        this.point_ID=  _pointID;
    }
    public String getDescription() {
        return  description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
