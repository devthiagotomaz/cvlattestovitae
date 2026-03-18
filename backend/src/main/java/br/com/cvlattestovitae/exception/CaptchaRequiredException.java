package br.com.cvlattestovitae.exception;

/**
 * Thrown when the CNPq Lattes page requires a CAPTCHA to be solved
 * before rendering the curriculum content.
 */
public class CaptchaRequiredException extends RuntimeException {

    private final String captchaUrl;

    public CaptchaRequiredException(String captchaUrl) {
        super("A página do Lattes requer resolução de captcha.");
        this.captchaUrl = captchaUrl;
    }

    /** @return the URL that triggered the CAPTCHA challenge */
    public String getCaptchaUrl() {
        return captchaUrl;
    }
}
