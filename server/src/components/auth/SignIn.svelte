<script>
  import { stores } from "@sapper/app";
  import { createEventDispatcher } from "svelte";
  import { slide } from "svelte/transition";

  import Textfield from "Textfield.svelte";
  import SubmitButton from "./SubmitButton.svelte";

  import { postApi } from "requests";

  export let element;

  const dispatch = createEventDispatcher();
  const { session } = stores();

  let usernameEntered = false;
  let passwordEntered = false;
  let wrongPassword = false;
  let username = "";
  let password = "";
  let usernameError;
  let passwordError;
  let promise;

  const checkUsernameEntered = () => {
    if (username.length > 0 && !usernameEntered) {
      usernameEntered = true;
    }
  };

  const checkPasswordEntered = () => {
    if (password.length > 0 && !passwordEntered) {
      passwordEntered = true;
    }
  };

  const checkWrongPassword = () => {
    if (wrongPassword) {
      wrongPassword = false;
    }
  };

  $: {
    checkUsernameEntered();
    checkWrongPassword();

    if (usernameEntered) {
      if (username.length === 0) {
        usernameError = "Заполните это поле.";
      } else if (/[^0-9a-zA-Z#$*_]/.test(username)) {
        usernameError =
          "Логин может состоять только из английских букв, цифр и знаков" +
          " #, $, *, _.";
      } else {
        usernameError = "";
      }
    }
  }

  $: {
    checkPasswordEntered();
    checkWrongPassword();

    if (passwordEntered) {
      if (password.length === 0) {
        passwordError = "Заполните это поле.";
      } else if (password.length < 8) {
        passwordError = "Пароль должен состоять как минимум из 8 символов.";
      } else {
        passwordError = "";
      }
    }
  }

  $: disabled = passwordError || usernameError || wrongPassword;

  const signin = (e) => {
    if (passwordEntered && usernameEntered) {
      if (!disabled) {
        promise = postApi($session.apiUrl + "/users/signin", {
          username,
          password,
        });

        promise.then(
          (json) => {
            console.log(json);
            dispatch("signed", json);
          },
          (e) => {
            if (e.status === 401) {
              promise = null;
              wrongPassword = true;
            }
          }
        );
      }
    } else {
      usernameEntered = true;
      passwordEntered = true;
    }
  };
</script>

<style lang="scss">
  @import "colors";

  .signin {
    padding: 3rem 1rem;
    text-align: right;

    .await {
      font-weight: 700;
      color: $color-green;
    }

    .error {
      font-size: .75rem;
      text-align: center;
      color: $color-error;
    }
  }
</style>

<form bind:this={element} class="signin">
  <div class="fields">
    <Textfield
      counter={32}
      bind:value={username}
      error={usernameError}
      label="Логин" />
    <Textfield
      counter={128}
      type="password"
      bind:value={password}
      error={passwordError}
      label="Пароль" />
  </div>
  {#if promise}
    {#await promise}
      <p class="await">Ждём ответа...</p>
    {:then resolved}
      <p class="await">Перенаправляем...</p>
    {:catch e}
      <p class="error">
        К сожалению, произошла какая-то ошибка. Пожалуйста, попробуйте снова
        через пару минут или обратитесь к администратору.
      </p>
    {/await}
  {:else if wrongPassword}
    <p class="error" transition:slide>Неправильный логин или пароль.</p>
    <SubmitButton on:click={signin} disabled label="Войти" />
  {:else}
    <SubmitButton on:click={signin} {disabled} label="Войти" />
  {/if}
</form>
