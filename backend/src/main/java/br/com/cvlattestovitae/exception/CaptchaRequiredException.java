package br.com.cvlattestovitae.exception;

/**
 * Thrown when the CNPq Lattes page requires a CAPTCHA to be solved
 * before rendering the curriculum content.
 */
public class CaptchaRequiredException extends RuntimeException {

    public CaptchaRequiredException() {
        super("A página do Lattes requer resolução de captcha.");
    }
}
