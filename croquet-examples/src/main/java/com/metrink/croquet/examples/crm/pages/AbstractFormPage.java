package com.metrink.croquet.examples.crm.pages;

import java.lang.reflect.InvocationTargetException;

import javax.persistence.EntityManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metrink.croquet.examples.crm.data.Identifiable;
import com.metrink.croquet.wicket.CroquetPage;

/**
 * An abstract page that has a form which is updated by the extending page.
 *
 * @param <T> the type of the object the form represents.
 */
public abstract class AbstractFormPage<T extends Identifiable> extends CroquetPage {
    private static final long serialVersionUID = -3136579068845135299L;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFormPage.class);

    private final Class<T> beanClass;
    private IModel<T> formModel;
    private Form<T> form;
    private Button updateSaveButton;

/*    
    public AbstractFormPage() {
        beanClass = null;
        entityManager = null;
    }
*/
    /**
     * Constructor.
     * @param beanClass the class of the type objects stored in the form.
     * @param entityManager the {@link EntityManager} that will create and update the objects.
     */
    public AbstractFormPage(final Class<T> beanClass, final EntityManager entityManager) {
        this.beanClass = beanClass;

        try {
            this.formModel = Model.of(createNewBean());
        } catch (final InstantiationException e) {
            LOG.error("Error creating new bean: {}", e.getMessage());
            getSession().error("Error creating new bean: " + e.getMessage());

            AbstractFormPage.this.setResponsePage(AbstractFormPage.this.getClass());
            return;
        }

        form = new Form<T>("form", this.formModel) {
            private static final long serialVersionUID = -3564662582656203263L;

            @Override
            protected void onSubmit() {
                final T bean = getModelObject();

                LOG.debug("Entity manager: {}", entityManager.isOpen());

                try {
                    entityManager.getTransaction().begin();

                    // decide if we're creating a new one or updating
                    if(bean.getId() == null) {
                        entityManager.persist(bean);
                        LOG.debug("{} created", bean);
                        getSession().success(bean + " was added");
                    } else { // updating
                        entityManager.merge(bean);
                        LOG.debug("{} updated", bean);
                        getSession().success(bean + " was updated");
                    }

                    entityManager.getTransaction().commit();
                } catch(final HibernateException e) {
                    final String msg = e.getMessage();

                    LOG.error("Error communicating with database: {}", msg);
                    getSession().error("Error communicating with database: " + msg);

                    entityManager.getTransaction().rollback();
                }

                // get any page params
                final PageParameters params = getRedirectPageParameters();

                // just redirect to ourself
                this.setResponsePage(AbstractFormPage.this.getClass(), params);
            }
        };

        // add in all the form's components
        addFormComponents(form);

        updateSaveButton = new Button("update-button", Model.of("New"));
        form.add(updateSaveButton);

        form.add(new AjaxButton("cancel-button", form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> submitForm) {
                LOG.debug("Clearing form");

                // set the model to a new object
                try {
                    formModel.setObject(createNewBean());
                } catch (final InstantiationException e) {
                    LOG.error("Error creating new bean: {}", e.getMessage());
                    getSession().error("Error creating new bean: " + e.getMessage());

                    AbstractFormPage.this.setResponsePage(AbstractFormPage.this.getClass());
                    return;
                }

                form.modelChanged();
                form.clearInput();

                target.add(form);

                // update the button
                updateSaveButton.setModel(Model.of("New"));
                target.add(updateSaveButton);
            }
        }.setDefaultFormProcessing(false));

        add(form);
    }

    /**
     * Callback used to add all the components of the form.
     * @param form the form to add components to.
     */
    protected abstract void addFormComponents(final Form<T> form);

    /**
     * Returns the form's model.
     * @return the form's model.
     */
    protected IModel<T> getFormModel() {
        return formModel;
    }

    /**
     * Allows extending classes the ability to inject any page parameters for the redirect.
     * @return blank {@link PageParameters}.
     */
    protected PageParameters getRedirectPageParameters() {
        return new PageParameters();
    }

    /**
     * Creates a new bean.
     * @return a new bean.
     * @throws InstantiationException if anything goes wrong.
     */
    protected T createNewBean() throws InstantiationException {
        try {
            return beanClass.getConstructor().newInstance();
        } catch (final InstantiationException |
                 IllegalAccessException |
                 IllegalArgumentException |
                 InvocationTargetException |
                 NoSuchMethodException |
                 SecurityException e) {
            // fold it all into one type of exception
            throw new InstantiationException(e.getMessage());
        }
    }

    /**
     * Sets the form to the bean passed in, updating the form via AJAX.
     * @param bean the bean to set the form to.
     * @param target the target of whatever triggers the form fill action.
     */
    protected void setForm(final T bean, final AjaxRequestTarget target) {
        LOG.debug("Setting form to {}", bean);

        // update the model with the new bean
        formModel.setObject(bean);

        // add the form to the target
        target.add(form);

        // update the button
        updateSaveButton.setModel(Model.of("Update"));
        target.add(updateSaveButton);
    }

}
