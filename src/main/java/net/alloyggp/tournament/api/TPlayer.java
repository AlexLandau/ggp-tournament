package net.alloyggp.tournament.api;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

@Immutable
public class TPlayer {
    private final String id;

    private TPlayer(String id) {
        Preconditions.checkArgument(!id.contains(","), "Player names should not contain commas");
        Preconditions.checkArgument(!id.contains("\\"), "Player names should not contain backslashes");
        this.id = id;
    }

    public static TPlayer create(String id) {
        return new TPlayer(id);
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TPlayer other = (TPlayer) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Player [id=" + id + "]";
    }
}
