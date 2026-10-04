package com.pokemontd;
public class TurretMarker extends Moveable {
	private static Coord grassCoord = new Coord(100, 100);
    private static GrassMap grassMap = new GrassMap();
	private boolean isHover = false;

	public TurretMarker(String location, int xstart, int ystart, int speed) {
		super(location, xstart, ystart, speed);
		this.getImageView().setMouseTransparent(true);
		this.getImageView().setVisible(false);
	}

	public void hover(int x, int y) {
		Coord grass = grassMap.isPlaceable(new Coord(x, y));
		isHover = (grass.getX() > 0);
		if (isHover) {
			grassCoord = grass;
			this.setX(grass.getX());
			this.setY(grass.getY());
			this.getImageView().setVisible(true);
		} else {
			this.getImageView().setVisible(false);
		}
	}

	public boolean isHovering() {
		return isHover;
	}

	public Coord getGrassCoord() {
		return grassCoord;
	}
}


            // Coord grass = grassMap.isPlaceable(new Coord(x, y));
            // boolean isHover = (grass.getX() > 0 ? true : false);
            // Var.isHovering(isHover);
            // Var.grassCoord(grass);
            // turretMark.setX(grass.getX());
            // turretMark.setY(grass.getY());
            // turretMark.update(cursor);