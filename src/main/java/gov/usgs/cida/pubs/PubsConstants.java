package gov.usgs.cida.pubs;

public final class PubsConstants {

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String MIME_TYPE_APPLICATION_JSON = "application/json";
    
    public static final String SPACES_OR_NUMBER_REGEX = "^ *\\d*$";
    
    public static final String FOUR_DIGIT_REGEX = "^\\d{4}$";
    
    /** The default username for anonymous access. */
    public static final String ANONYMOUS_USER = "anonymous";

    //SQL config for single search 
    public static final String SEARCH_TERM_ORDERBY = "publication_year";
    public static final String SEARCH_TERM_ORDERBY_DIR = "DESC";

    private PubsConstants() {
    };

}
