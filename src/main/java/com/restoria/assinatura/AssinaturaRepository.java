package com.restoria.assinatura;

import com.restoria.shared.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    Optional<Assinatura> findByUsuario(Usuario usuario);

    Optional<Assinatura> findByStripeCustomerId(String stripeCustomerId);

    Optional<Assinatura> findByStripeSubscriptionId(String stripeSubscriptionId);
}
