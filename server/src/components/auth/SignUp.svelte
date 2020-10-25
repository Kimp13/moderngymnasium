<script>
  import { stores } from "@sapper/app";
  import { fly } from "svelte/transition";
  import { createEventDispatcher } from "svelte";

  import Textfield from "../Textfield.svelte";
  import SubmitButton from "./SubmitButton.svelte";
  import Select from "../Select.svelte";

  import { postApi, getApiResponse } from "../../../utils/requests.js";
  import Loader from "../Loader.svelte";

  export let info;
  export let element;

  const dispatch = createEventDispatcher();
  const { session } = stores();

  let firstNameEntered = false;
  let lastNameEntered = false;
  let usernameEntered = false;
  let passwordEntered = false;
  let passwordRepeatEntered = false;
  let wrongUsername = false;
  let firstName = "";
  let lastName = "";
  let username = "";
  let password = "";
  let passwordRepeat = "";
  let firstNameError;
  let lastNameError;
  let usernameError;
  let passwordError;
  let passwordRepeatError;
  let roleSelect;
  let schoolSelect;
  let classSelect;
  let classPromise;
  let classPromiseSchool;
  let fieldsEntered;
  let signUpPromise;

  const checkFirstNameEntered = () => {
    if (firstName.length > 0 && !firstNameEntered) {
      firstNameEntered = true;
    }
  };
  const checkLastNameEntered = () => {
    if (lastName.length > 0 && !lastNameEntered) {
      lastNameEntered = true;
    }
  };
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
  const checkPasswordRepeatEntered = () => {
    if (passwordRepeat.length > 0 && !passwordRepeatEntered) {
      passwordRepeatEntered = true;
    }
  };
  const checkWrongUsername = () => {
    if (wrongUsername) {
      wrongUsername = false;
    }
  };
  const updateClasses = () => {
    if (
      !(info.classes || classPromise) ||
      (classPromiseSchool && classPromiseSchool !== schoolSelect)
    ) {
      classSelect = -1;
      classPromiseSchool = schoolSelect;
      classPromise = getApiResponse(`${$session.apiUrl}/classes/get`, {
        schoolId: info.schools[schoolSelect].id,
      });

      classPromise.then((classes) => {
        info.classes = classes;
      });
    }
  };
  const mapFunction = (entity) => entity.name;

  $: classSelectError =
    fieldsEntered && roleSelect < 0 ? "Выберите значение." : false;
  $: roleSelectError =
    fieldsEntered && roleSelect < 0 ? "Выберите значение." : false;
  $: schoolSelectError =
    fieldsEntered && schoolSelect < 0 ? "Выберите значение." : false;
  $: classEnabled = schoolSelect >= 0;
  $: if (classEnabled) {
    updateClasses();
  }

  $: {
    checkFirstNameEntered();

    if (firstNameEntered) {
      if (firstName.length === 0) {
        firstNameError = "Заполните это поле.";
      } else if (/[^а-яА-ЯёЁ]/.test(firstName)) {
        firstNameError = "Имя может состоять только из русских букв.";
      } else {
        firstNameError = "";
      }
    }
  }

  $: {
    checkLastNameEntered();

    if (lastNameEntered) {
      if (lastName.length === 0) {
        lastNameError = "Заполните это поле.";
      } else if (/[^а-яА-ЯёЁ]/.test(lastName)) {
        lastNameError = "Фамилия может состоять только из русских букв.";
      } else {
        lastNameError = "";
      }
    }
  }

  $: {
    checkUsernameEntered();
    checkWrongUsername();

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
  $: {
    checkPasswordRepeatEntered();

    if (passwordRepeatEntered) {
      if (passwordRepeat.length === 0) {
        passwordRepeatError = "Заполните это поле.";
      } else if (passwordRepeat !== password) {
        passwordRepeatError = "Пароли не совпадают.";
      } else {
        passwordRepeatError = "";
      }
    }
  }
  $: disabled = Boolean(
    usernameError ||
      passwordError ||
      passwordRepeatError ||
      roleSelectError ||
      schoolSelectError ||
      classSelectError
  );

  const signup = (e) => {
    e.preventDefault();

    if (
      usernameEntered &&
      passwordEntered &&
      passwordRepeatEntered &&
      roleSelect >= 0 &&
      schoolSelect >= 0 &&
      classSelect >= 0
    ) {
      if (!disabled) {
        signUpPromise = postApi($session.apiUrl + "/users/signup", {
          firstName,
          lastName,
          username,
          password,
          roleId: info.roles[roleSelect].id,
          classId: info.classes[classSelect].id,
        });

        signUpPromise.then(
          (json) => {
            dispatch("signed", json);
          },
          (e) => {
            if (e.status === 403) {
              signUpPromise = null;
              wrongUsername = true;
            }
          }
        );
      }
    } else {
      firstNameEntered = true;
      lastNameEntered = true;
      usernameEntered = true;
      passwordEntered = true;
      passwordRepeatEntered = true;
      fieldsEntered = true;
      classSelect = -5;
    }
  };
</script>

<style lang="scss">
  @import "../../theme/global";

  form {
    display: flex;
    flex-wrap: wrap;
    color: $mdc-theme-secondary;

    p.disabled {
      width: 100%;
      padding: 0.25rem 0.5rem;
      font-size: 0.8rem;
      color: $mdc-theme-secondary;
      text-align: center;
    }
  }
</style>

<form
  on:submit|preventDefault={() => 0}
  bind:this={element}
  class="signup"
  transition:fly={{ x: 300, duration: 300 }}>
  <div class="fields">
    <Textfield bind:value={firstName} error={firstNameError} label="Имя" />
    <Textfield bind:value={lastName} error={lastNameError} label="Фамилия" />
  </div>
  <div class="fields">
    <Textfield bind:value={username} error={usernameError} label="Логин" />
  </div>
  <div class="fields">
    <Textfield
      type="password"
      bind:value={password}
      error={passwordError}
      label="Пароль" />
    <Textfield
      type="password"
      bind:value={passwordRepeat}
      error={passwordRepeatError}
      label="Повтор пароля" />
  </div>
  <div class="fields">
    <Select
      label="Роль"
      options={info.roles.map(mapFunction)}
      error={roleSelectError}
      bind:selectedIndex={roleSelect} />
    <Select
      label="Школа"
      options={info.schools.map(mapFunction)}
      error={schoolSelectError}
      bind:selectedIndex={schoolSelect} />
    {#if classEnabled}
      {#await classPromise}
        <Loader />
      {:then resolved}
        <Select
          label="Класс"
          options={info.classes.map((cls) => `${cls.grade}${cls.letter}`)}
          error={classSelectError}
          bind:selectedIndex={classSelect} />
      {:catch e}
        <p class="error">
          К сожалению, произошла какая-то&nbsp; ошибка. Пожалуйста, попробуйте
          снова через&nbsp; пару минут или обратитесь к администратору.
        </p>
      {/await}
    {:else}
      <p class="disabled">Выберите школу, чтобы выбрать класс.</p>
    {/if}
  </div>
  {#if signUpPromise}
    {#await signUpPromise}
      <p class="await">Ждём ответа...</p>
    {:then resolved}
      <p class="await">Перенаправляем...</p>
    {:catch e}
      <p class="error">
        К сожалению, произошла какая-то&nbsp; ошибка. Пожалуйста, попробуйте
        снова через&nbsp; пару минут или обратитесь к администратору.
      </p>
    {/await}
  {:else if wrongUsername}
    <p class="error">Этот логин уже занят.</p>
    <SubmitButton
      disabled
      icon="how_to_reg"
      label="регистрация"
      on:click={signup} />
  {:else}
    <SubmitButton
      {disabled}
      icon="how_to_reg"
      label="регистрация"
      on:click={signup} />
  {/if}
</form>
