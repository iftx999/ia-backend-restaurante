-- RF-16: alertas proativos. Guarda no proprio relatorio se algum indicador
-- (margem de prato ou perda de insumo) saiu da faixa esperada do usuario,
-- pra nao precisar reabrir/recalcular o relatorio so pra saber se ha alerta
-- (usado no banner do chat, ver AnaliseService.obterAlertaMaisRecente).
ALTER TABLE relatorio
    ADD COLUMN tem_alerta boolean NOT NULL DEFAULT false,
    ADD COLUMN quantidade_alertas integer NOT NULL DEFAULT 0;
