package cx.hell.android.lib.pagesview;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Tile definition.
 * Can be used as a key in maps (hashable, comparable).
 */
public class Tile implements Parcelable{

	/**
	 * X position of tile in pixels.
	 */
	private int x;
	
	/**
	 * Y position of tile in pixels.
	 */
	private int y;
	
	private int zoom;
	private int page;
	private int rotation;
	
	private int prefXSize;
	private int prefYSize;

    public boolean isVisible=false;
    Rect rect;
	
	//int _hashCode;
	
	public Tile(int page, int zoom, int x, int y, int rotation, int prefXSize, int prefYSize) {
		this.prefXSize = prefXSize;
		this.prefYSize = prefYSize;
		this.page = page;
		this.zoom = zoom;
		this.x = x;
		this.y = y;
		this.rotation = rotation;
        //System.out.println("ctor :"+toString());
        isVisible=true;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect=rect;
    }

    public String toString() {
		return "Tile(" +
			this.page + ", " +
			this.zoom + ", " +
			this.x + ", " +
			this.y + ", " +
            this.prefXSize + ", " +
            this.prefYSize + ", " +
            this.rect + ", " +
			this.rotation + ")";
	}
	
	/*@Override
	public int hashCode() {
		return this._hashCode;
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof Tile)) return false;
		Tile t = (Tile) o;
		return (
					this._hashCode == t._hashCode
					&& this.page == t.page
					&& this.zoom == t.zoom
					&& this.x == t.x
					&& this.y == t.y
					&& this.rotation == t.rotation
				);
	}*/

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o==null||getClass()!=o.getClass()) {
            //System.out.println("task :"+getClass()+" that:"+o.getClass());
            return false;
        }

        Tile tile=(Tile) o;

        if (page!=tile.page){
            //System.out.println("task :"+page+" that:"+tile.page);
            return false;
        }
        if (prefXSize!=tile.prefXSize){
            //System.out.println("task :"+prefXSize+" that:"+tile.prefXSize);
            return false;
        }
        if (prefYSize!=tile.prefYSize){
            //System.out.println("task :"+prefYSize+" that:"+tile.prefYSize);
            return false;
        }
        if (x!=tile.x){
            //System.out.println("task :"+x+" that:"+tile.x);
            return false;
        }
        if (y!=tile.y){
            //System.out.println("task :"+y+" that:"+tile.y);
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result=x;
        result=31*result+y;
        result=31*result+page;
        result=31*result+prefXSize;
        result=31*result+prefYSize;
        return result;
    }

    public int getPage() {
		return this.page;
	}
	
	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getZoom() {
		return this.zoom;
	}

	public int getRotation() {
		return this.rotation;
	}
	
	public int getPrefXSize() {
		return this.prefXSize;
	}

	public int getPrefYSize() {
		return this.prefYSize;
	}

    //===============================================

    public static final Creator<Tile> CREATOR=new Creator<Tile>() {
        public Tile createFromParcel(Parcel in) {
            return new Tile(in);
        }

        public Tile[] newArray(int size) {
            return new Tile[size];
        }
    };

    private Tile(Parcel in) {
        x=in.readInt();
        y=in.readInt();
        zoom=in.readInt();
        page=in.readInt();
        rotation=in.readInt();
        prefXSize=in.readInt();
        prefYSize=in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(x);
        dest.writeInt(y);
        dest.writeInt(zoom);
        dest.writeInt(page);
        dest.writeInt(rotation);
        dest.writeInt(prefXSize);
        dest.writeInt(prefYSize);
    }
}
