package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.FiscalJurisdiction;
import com.howners.gestion.domain.accounting.FiscalRegime;
import com.howners.gestion.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sélectionne le moteur fiscal correspondant à (juridiction, régime). Tous les
 * {@link FiscalEngine} déclarés en beans Spring sont enregistrés automatiquement :
 * ajouter une juridiction n'exige aucune modification ici.
 */
@Service
public class FiscalEngineResolver {

    private final Map<String, FiscalEngine> engines;

    public FiscalEngineResolver(List<FiscalEngine> engineList) {
        this.engines = engineList.stream()
                .collect(Collectors.toMap(e -> key(e.jurisdiction(), e.regime()), Function.identity()));
    }

    public FiscalEngine resolve(FiscalJurisdiction jurisdiction, FiscalRegime regime) {
        FiscalEngine engine = engines.get(key(jurisdiction, regime));
        if (engine == null) {
            throw new BadRequestException(
                    "Aucun moteur fiscal disponible pour " + jurisdiction + " / " + regime);
        }
        return engine;
    }

    public FiscalEngine resolve(FiscalActivity activity) {
        return resolve(activity.getJurisdiction(), activity.getRegime());
    }

    private static String key(FiscalJurisdiction j, FiscalRegime r) {
        return j.name() + "|" + r.name();
    }
}
