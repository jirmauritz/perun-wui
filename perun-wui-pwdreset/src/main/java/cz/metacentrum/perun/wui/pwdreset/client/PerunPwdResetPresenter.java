package cz.metacentrum.perun.wui.pwdreset.client;

import com.google.gwt.core.client.*;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import cz.metacentrum.perun.wui.client.PerunPresenter;
import cz.metacentrum.perun.wui.client.resources.PerunConfiguration;
import cz.metacentrum.perun.wui.client.resources.PerunSession;
import cz.metacentrum.perun.wui.client.resources.PlaceTokens;
import cz.metacentrum.perun.wui.json.JsonEvents;
import cz.metacentrum.perun.wui.json.managers.RegistrarManager;
import cz.metacentrum.perun.wui.model.BasicOverlayObject;
import cz.metacentrum.perun.wui.model.PerunException;
import cz.metacentrum.perun.wui.pwdreset.client.resources.PerunPwdResetTranslation;
import cz.metacentrum.perun.wui.widgets.PerunButton;
import cz.metacentrum.perun.wui.widgets.boxes.ExtendedTextBox;
import cz.metacentrum.perun.wui.widgets.recaptcha.RecaptchaWidget;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.FormType;
import org.gwtbootstrap3.client.ui.constants.IconPosition;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.client.ui.constants.ValidationState;

/**
 * @author Ondrej Velisek <ondrejvelisek@gmail.com>
 */
public class PerunPwdResetPresenter extends PerunPresenter<PerunPwdResetPresenter.MyView, PerunPwdResetPresenter.MyProxy> {

	private boolean captchaOK = !"non".equals(PerunSession.getInstance().getRpcServer());
	private PerunPwdResetTranslation translation = GWT.create(PerunPwdResetTranslation.class);

	@ProxyStandard
	public interface MyProxy extends Proxy<PerunPwdResetPresenter> {
	}

	public interface MyView extends View {

		void hideNavbar();

	}

	private PlaceManager placeManager = PerunSession.getPlaceManager();

	@Inject
	PerunPwdResetPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
		super(eventBus, view, proxy);
	}

	@Override
	protected void onBind() {

		super.onBind();

		if (PerunSession.getInstance().getUser() == null && !PerunSession.getInstance().getRpcServer().equals("non")) {
			placeManager.revealPlace(new PlaceRequest.Builder().nameToken(PlaceTokens.NOT_USER).build());
			return;
		} else if (PerunSession.getInstance().getUser() == null &&
				PerunSession.getInstance().getRpcServer().equals("non") &&
				!Window.Location.getParameterMap().keySet().contains("m") &&
				!Window.Location.getParameterMap().keySet().contains("i")) {
			placeManager.revealPlace(new PlaceRequest.Builder().nameToken(PlaceTokens.NOT_USER).build());
			return;
		}

		// FIXME - very raw implementation to ensure back compatibility with old Registrar
		// FIXME - we must implement re-captcha 2 widget

		if (!captchaOK) {

			final String reCaptchaPublicKey = PerunConfiguration.getReCaptchaPublicKey();

			if (reCaptchaPublicKey != null && !reCaptchaPublicKey.isEmpty()) {

				ScriptInjector.fromUrl("https://www.google.com/recaptcha/api/js/recaptcha_ajax.js").setCallback(
						new Callback<Void, Exception>() {
							public void onFailure(Exception reason) {
								Window.alert("Script load failed.");
							}

							public void onSuccess(Void result) {

								final Modal modal = new Modal();
								modal.setRemoveOnHide(true);
								modal.setDataBackdrop(ModalBackdrop.STATIC);

								ModalHeader header = new ModalHeader();
								header.setTitle(translation.pleaseVerifyCaptcha());
								header.setClosable(false);
								ModalBody body = new ModalBody();
								ModalFooter footer = new ModalFooter();

								final RecaptchaWidget captcha = new RecaptchaWidget(reCaptchaPublicKey, PerunConfiguration.getCurrentLocaleName(), "clean");

								final PerunButton verify = new PerunButton(translation.continueButton(), IconType.CHEVRON_RIGHT);
								verify.setType(ButtonType.SUCCESS);
								verify.setIconFixedWidth(true);
								verify.setIconPosition(IconPosition.RIGHT);

								final ExtendedTextBox response = new ExtendedTextBox();
								response.setPlaceholder(translation.captchaAnswer());
								captcha.setOwnTextBox(response);
								response.addStyleName("pull-left");
								response.getElement().setAttribute("style", "margin-right: 5px; width: 70%;");

								Form form = new Form();
								form.setType(FormType.INLINE);
								form.setWidth("100%");
								final FormGroup group = new FormGroup();
								group.setWidth("100%");
								group.add(response);
								group.add(verify);
								form.add(group);

								response.addKeyDownHandler(new KeyDownHandler() {
									@Override
									public void onKeyDown(KeyDownEvent event) {
										if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
											verify.click();
										}
									}
								});

								verify.addClickHandler(new ClickHandler() {
									@Override
									public void onClick(ClickEvent event) {
										RegistrarManager.verifyCaptcha(captcha.getChallenge(), captcha.getResponse(), new JsonEvents() {
											@Override
											public void onFinished(JavaScriptObject result) {
												verify.setProcessing(false);
												BasicOverlayObject bt = result.cast();
												if (bt.getBoolean()) {
													group.setValidationState(ValidationState.SUCCESS);
													modal.hide();
												} else {
													group.setValidationState(ValidationState.ERROR);
												}
											}

											@Override
											public void onError(PerunException error) {
												verify.setProcessing(false);
											}

											@Override
											public void onLoadingStart() {
												verify.setProcessing(true);
											}
										});
									}
								});


								body.add(captcha);

								footer.add(form);

								modal.add(header);
								modal.add(body);
								modal.add(footer);

								modal.show();

								Scheduler.get().scheduleDeferred(new Command() {
									@Override
									public void execute() {
										response.setFocus(true);
									}
								});

							}
						}).setWindow(ScriptInjector.TOP_WINDOW).inject();

			} else {
				// captcha is not ok and req params are missing in URL
				placeManager.revealErrorPlace("");
			}
		}

	}

	private int resets = 0;

	@Override
	protected void onReset() {
		super.onReset();

		// Because NavbarCollapse.hide() doesn't work properly. It shows the NavbarCollapse on application startup.
		resets++;
		if (resets > 2) {
			getView().hideNavbar();
		}

	}

}
