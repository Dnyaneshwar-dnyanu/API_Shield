package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.util.StringUtils;

/**
 * Compatibility bridge for Spring Cloud Gateway 4.2.0 running on Spring Boot 4 / Spring 7.
 * Restores the deprecated/removed setMatchOptionalTrailingSeparator() method.
 */
public class PathPatternParser {

    private boolean caseSensitive = true;

    private PathContainer.Options pathOptions = PathContainer.Options.HTTP_PATH;

    public static final PathPatternParser defaultInstance = new PathPatternParser() {
        @Override
        public void setCaseSensitive(boolean caseSensitive) {
            throw new UnsupportedOperationException("This instance is read-only");
        }

        @Override
        public void setPathOptions(PathContainer.Options pathOptions) {
            throw new UnsupportedOperationException("This instance is read-only");
        }
    };

    public PathPatternParser() {
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    public void setPathOptions(PathContainer.Options pathOptions) {
        this.pathOptions = pathOptions;
    }

    public PathContainer.Options getPathOptions() {
        return this.pathOptions;
    }

    public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
        // Compatibility no-op
    }

    public boolean isMatchOptionalTrailingSeparator() {
        return true;
    }

    public String initFullPathPattern(String pattern) {
        return (StringUtils.hasLength(pattern) && !pattern.startsWith("/") ? "/" + pattern : pattern);
    }

    public PathPattern parse(String pathPattern) throws PatternParseException {
        return new InternalPathPatternParser(this).parse(pathPattern);
    }
}
