package net.alloyggp.swiss.api;

public class Game {
	private final String source;
	private final String id;

	private Game(String source, String id) {
		this.source = source;
		this.id = id;
	}

	public static Game create(String source, String id) {
		return new Game(source, id);
	}

	public String getSource() {
		return source;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Game other = (Game) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Game [source=" + source + ", id=" + id + "]";
	}
}
