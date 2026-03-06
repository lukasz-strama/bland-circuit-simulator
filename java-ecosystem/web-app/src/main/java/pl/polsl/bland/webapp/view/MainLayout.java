package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route("")
@PageTitle("Bland Circuit Simulator")
public class MainLayout extends VerticalLayout {

    public MainLayout() {
        add(new H1("Bland Circuit Simulator"));
        add(new Paragraph("Schematic editor and simulation results will be rendered here."));
        setSizeFull();
    }
}
