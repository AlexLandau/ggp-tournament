package net.alloyggp.tournament.internal;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import net.alloyggp.tournament.api.TGame;

@Immutable
public class Game implements TGame {
    private final String id;
    private final String url;
    private final int numRoles;
    private final boolean fixedSum;

    private Game(String id, String url, int numRoles, boolean fixedSum) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(url));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id));
        Preconditions.checkArgument(numRoles > 0);
        this.id = id;
        this.url = url;
        this.numRoles = numRoles;
        this.fixedSum = fixedSum;
    }

    public static Game create(String id, String url, int numRoles, boolean fixedSum) {
        return new Game(id, url, numRoles, fixedSum);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getNumRoles() {
        return numRoles;
    }

    @Override
    public boolean isFixedSum() {
        return fixedSum;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fixedSum ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + numRoles;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        Game other = (Game) obj;
        if (fixedSum != other.fixedSum) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (numRoles != other.numRoles) {
            return false;
        }
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Game [id=" + id + ", url=" + url + ", numRoles=" + numRoles + ", fixedSum=" + fixedSum + "]";
    }
}
