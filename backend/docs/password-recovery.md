# Recuperação e alteração de senha

## Contratos atuais

### Recuperação por e-mail

`PATCH /usuarios/email/{email}/senha`

O corpo contém somente a nova senha. `novaSenha` é obrigatória, não pode ser vazia ou composta apenas por espaços e deve ter pelo menos oito caracteres.

```json
{
  "novaSenha": "NovaSenha123"
}
```

### Alteração por ID

`PATCH /usuarios/{idUsuario}/senha`

Esse fluxo exige a senha atual e uma nova senha válida.

```json
{
  "senhaAtual": "SenhaAtual123",
  "novaSenha": "NovaSenha123"
}
```

Ambos retornam os dados públicos do usuário sem incluir a senha. Requisições inválidas retornam `400`; usuário não encontrado retorna `404`.

## Limitações de segurança

O endpoint de recuperação por e-mail identifica a conta apenas pelo endereço informado na URL. Não há token, código de verificação ou outra prova de controle desse endereço no fluxo atual. Portanto, qualquer cliente que alcance esse endpoint pode tentar redefinir a senha de outra pessoa. Não exponha esse endpoint como recuperação pública até que haja uma verificação de identidade.

Recomenda-se implementar um token aleatório, de uso único e validade curta, enviado ao endereço já cadastrado. Armazene apenas o hash do token, consuma-o atomicamente e limite tentativas. A resposta da etapa que solicita recuperação também deve evitar revelar se o endereço existe.

O repositório não possui um `PasswordEncoder` nem um fluxo de login implementado; cadastro e redefinição gravam a senha recebida diretamente. O armazenamento atual não é seguro. Uma migração para hash deve abranger cadastro, login e todas as operações de alteração, além de definir uma estratégia para as senhas já existentes. Não introduza codificação apenas neste endpoint, pois isso tornaria a verificação de senha inconsistente com o restante do sistema.
