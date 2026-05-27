package app.controllers;

import app.DAO.CourtsDAO;
import app.DAO.LocationsDAO;
import app.entities.location.Court;
import app.entities.location.Location;
import app.views.admin.locations.CreateLocationView;

import javax.swing.*;
import java.util.List;

public class LocationController {
    private CreateLocationView view; //форма для створення(редагування) місця проведення
    private LocationsDAO locationsDAO = LocationsDAO.getInstance(); //DAO локацій
    private CourtsDAO courtsDAO = CourtsDAO.getInstance(); // DAO кортів
    private Location locationToEdit; //місце проведення для редагування

    //конструктор для режиму збереження місця проведення
    public LocationController(CreateLocationView view) {
            this.view = view;
            initController();
    }

    //конструктор для режиму редагування місця проведення
    public LocationController(CreateLocationView view, Location locationToEdit) {
            this.view = view;
            this.locationToEdit = locationToEdit;
            initController();
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
            view.getSaveButton().addActionListener(e -> handleSave());
            view.getCancelButton().addActionListener(e -> view.dispose());
    }

    //збереження/оновлення місця проведення
    private void handleSave() {
        //зчитування та конвертація даних з форми
        Location location = (locationToEdit == null) ? new Location() : locationToEdit;

        location.setName(view.getName());
        location.setCity(view.getCity());
        location.setAddress(view.getAddress());
        location.setIsAvailable(view.isLocationAvailable());
        location.setCourts(view.getCourts());

        //внутрішня бізнес-валідація сутності
        List<String> errors = location.validate(locationsDAO);

        if (errors.isEmpty()) {
            try {
                //збереження в базу даних (створення або оновлення)
                if (locationToEdit == null) {
                    //гілка створення
                    locationsDAO.save(location);
                    JOptionPane.showMessageDialog(view, "Локацію створено!");
                } else {
                    //гілка оновлення
                    locationsDAO.update(location);

                    //оновлення кортів
                    for (Court c : location.getCourts()) {
                        if (c.getId() > 0) {
                            courtsDAO.update(c);
                        } else {
                            c.setLocationId(location.getId());
                            courtsDAO.save(c);
                        }
                    }
                    JOptionPane.showMessageDialog(view, "Дані локації та кортів оновлено!");
                }
                view.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(view, "Помилка: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(view, String.join("\n", errors));
        }
    }
}

