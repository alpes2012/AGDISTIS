package org.aksw.agdistis.model;

import java.util.ArrayList;

public class TwitterData {
    private String userId;
    private String userName;
    private String screenName;
    private ArrayList<Quote> quote;
    private ArrayList<Mention> mention;
    private ArrayList<Retweet> retweet;
    private ArrayList<Following> following;

    public class Quote {
        private String userName;
        private String screenName;
        private int times;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getScreenName() {
            return screenName;
        }

        public void setScreenName(String screenName) {
            this.screenName = screenName;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    public class Mention {
        private String userName;
        private String screenName;
        private int times;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getScreenName() {
            return screenName;
        }

        public void setScreenName(String screenName) {
            this.screenName = screenName;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    public class Retweet {
        private String userName;
        private String screenName;
        private int times;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getScreenName() {
            return screenName;
        }

        public void setScreenName(String screenName) {
            this.screenName = screenName;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    public class Following {
        private String userName;
        private String screenName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getScreenName() {
            return screenName;
        }

        public void setScreenName(String screenName) {
            this.screenName = screenName;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public ArrayList<Quote> getQuote() {
        return quote;
    }

    public void setQuote(ArrayList<Quote> quote) {
        this.quote = quote;
    }

    public ArrayList<Mention> getMention() {
        return mention;
    }

    public void setMention(ArrayList<Mention> mention) {
        this.mention = mention;
    }

    public ArrayList<Retweet> getRetweet() {
        return retweet;
    }

    public void setRetweet(ArrayList<Retweet> retweet) {
        this.retweet = retweet;
    }

    public ArrayList<Following> getFollowing() {
        return following;
    }

    public void setFollowing(ArrayList<Following> following) {
        this.following = following;
    }
}


