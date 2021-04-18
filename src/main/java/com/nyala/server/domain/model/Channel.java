package com.nyala.server.domain.model;

public class Channel {
    String name;
    String country;

    Channel(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public static ChannelBuilder builder() {
        return new ChannelBuilder();
    }

    public String getName() {
        return this.name;
    }

    public String getCountry() {
        return this.country;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Channel)) return false;
        final Channel other = (Channel) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$country = this.getCountry();
        final Object other$country = other.getCountry();
        if (this$country == null ? other$country != null : !this$country.equals(other$country)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Channel;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $country = this.getCountry();
        result = result * PRIME + ($country == null ? 43 : $country.hashCode());
        return result;
    }

    public String toString() {
        return "Channel(name=" + this.getName() + ", country=" + this.getCountry() + ")";
    }

    public static class ChannelBuilder {
        private String name;
        private String country;

        ChannelBuilder() {
        }

        public Channel.ChannelBuilder name(String name) {
            this.name = name;
            return this;
        }

        public Channel.ChannelBuilder country(String country) {
            this.country = country;
            return this;
        }

        public Channel build() {
            return new Channel(name, country);
        }

        public String toString() {
            return "Channel.ChannelBuilder(name=" + this.name + ", country=" + this.country + ")";
        }
    }
}
