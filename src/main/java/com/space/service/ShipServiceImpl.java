package com.space.service;

import com.space.BadRequestException;
import com.space.ShipNotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ShipServiceImpl implements ShipService {

    private ShipRepository shipRepository;

    @Autowired
    public void setShipRepository(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    public Page<Ship> getAllShips(Specification<Ship> specification, Pageable sortedByName) {
        return shipRepository.findAll(specification, sortedByName);
    }

    @Override
    public List<Ship> getAllShips(Specification<Ship> specification) {
        return shipRepository.findAll(specification);
    }

    @Override
    public Ship createShip(Ship ship) {

        if (ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null) {
            throw new BadRequestException("Params are bad");
        }

        checkShipParams(ship);

        if (ship.getUsed() == null) {
            ship.setUsed(false);
        }

        ship.setRating(calculateRating(ship));

        return shipRepository.saveAndFlush(ship);
    }

    @Override
    public Ship getShip(Long id) {
        if (!shipRepository.existsById(id)) {
            throw new ShipNotFoundException("Ship not found");
        }
        return shipRepository.findById(id).get();
    }

    @Override
    public Ship editShip(Long id, Ship ship) {
        checkShipParams(ship);

        Ship editShip = getShip(id);

        String name = ship.getName();
        if (name != null) {
            editShip.setName(name);
        }

        String planet = ship.getPlanet();
        if (planet != null) {
            editShip.setPlanet(planet);
        }

        ShipType shipType = ship.getShipType();
        if (shipType != null) {
            editShip.setShipType(shipType);
        }

        Date prodDate = ship.getProdDate();
        if (prodDate != null) {
            editShip.setProdDate(prodDate);
        }

        Double speed = ship.getSpeed();
        if (speed != null) {
            editShip.setSpeed(speed);
        }

        Boolean used = ship.getUsed();
        if (used != null) {
            editShip.setUsed(used);
        }

        Integer crewSize = ship.getCrewSize();
        if (crewSize != null) {
            editShip.setCrewSize(crewSize);
        }

        Double rating = calculateRating(editShip);
        editShip.setRating(rating);

        return shipRepository.save(editShip);
    }

    private Double calculateRating(Ship editShip) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(editShip.getProdDate());
        int year = calendar.get(Calendar.YEAR);

        BigDecimal rating = BigDecimal.valueOf(80 * editShip.getSpeed() * (editShip.getUsed() ? 0.5 : 1) / (3019 - year + 1));
        rating = rating.setScale(2, RoundingMode.HALF_UP);

        return rating.doubleValue();
    }

    private void checkShipParams(Ship ship) {

        String name = ship.getName();
        if (name != null && (name.length() < 1 || name.length() > 50)) {
            throw new BadRequestException("Incorect Ship.name");
        }

        String planet = ship.getPlanet();
        if (planet != null && (planet.length() < 1 || planet.length() > 50)) {
            throw new BadRequestException("Incorect Ship.planet");
        }

        Integer crewSize = ship.getCrewSize();
        if (crewSize != null && (crewSize < 1 || crewSize > 9999)) {
            throw new BadRequestException("Incorect Ship.crewSize");
        }

        Double speed = ship.getSpeed();
        if (speed != null && (speed < 0.01D || speed > 0.99D)) {
            throw new BadRequestException("Incorect Ship.speed");
        }

        Date prodDate = ship.getProdDate();
        if (prodDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(prodDate);
            int year = calendar.get(Calendar.YEAR);
            if (year < 2800 || year > 3019) {
                throw new BadRequestException("Incorect Ship.date");
            }
        }

    }

    @Override
    public void deleteById(Long id) {
        if (shipRepository.existsById(id)) {
            shipRepository.deleteById(id);
        } else {
            throw new ShipNotFoundException("Ship not found");
        }
    }

    @Override
    public Long checkAndParseId(String id) {
        if (id == null || id.equals("") || id.equals("0")) {
            throw new BadRequestException("Incorect ID");
        }

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID is not a number", e);
        }
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return (root, query, criteriaBuilder) -> name == null ? null : criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return (root, query, criteriaBuilder) -> planet == null ? null : criteriaBuilder.like(root.get("planet"), "%" + planet + "%");
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return (root, query, criteriaBuilder) -> shipType == null ? null : criteriaBuilder.equal(root.get("shipType"), shipType);
    }

    @Override
    public Specification<Ship> filterByDate(Long after, Long before) {
        return (root, query, criteriaBuilder) -> {

            if (after == null && before == null)
                return null;
            if (after == null) {
                Date before1 = new Date(before);
                return criteriaBuilder.lessThanOrEqualTo(root.get("prodDate"), before1);
            }
            if (before == null) {
                Date after1 = new Date(after);
                return criteriaBuilder.greaterThanOrEqualTo(root.get("prodDate"), after1);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(before));
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.SECOND, -1);

            Date before1 = cal.getTime();
            Date after1 = new Date(after);

            return criteriaBuilder.between(root.get("prodDate"), after1, before1);
        };
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return (root, query, criteriaBuilder) -> {
            if (isUsed == null) {
                return null;
            }

            Path<Boolean> rootIsUsed = root.get("isUsed");
            return isUsed ? criteriaBuilder.isTrue(rootIsUsed) : criteriaBuilder.isFalse(rootIsUsed);
        };
    }

    @Override
    public Specification<Ship> filterBySpeed(Double min, Double max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }

            Path<Double> speed = root.get("speed");

            if (min == null) {
                return criteriaBuilder.lessThanOrEqualTo(speed, max);
            }

            if (max == null) {
                return criteriaBuilder.greaterThanOrEqualTo(speed, min);
            }

            return criteriaBuilder.between(speed, min, max);
        };
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }

            Path<Integer> crewSize = root.get("crewSize");

            if (min == null) {
                return criteriaBuilder.lessThanOrEqualTo(crewSize, max);
            }

            if (max == null) {
                return criteriaBuilder.greaterThanOrEqualTo(crewSize, min);
            }

            return criteriaBuilder.between(crewSize, min, max);

        };

    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }

            Path<Double> rating = root.get("rating");

            if (min == null) {
                return criteriaBuilder.lessThanOrEqualTo(rating, max);
            }

            if (max == null) {
                return criteriaBuilder.greaterThanOrEqualTo(rating, min);
            }

            return criteriaBuilder.between(rating, min, max);
        };
    }
}
