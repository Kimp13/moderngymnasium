<script>
  import { createEventDispatcher } from "svelte";
  import { mdiLogin } from '@mdi/js';

  import Button from "Button.svelte";

  export let disabled = false;
  export let icon = mdiLogin;
  export let label = "Принять";

  const dispatch = createEventDispatcher();

  const dispatchClick = e => dispatch('click', e);
</script>

<style global lang="scss">
  .submit {
    margin: .25rem .5rem
  }
</style>

<Button
  class="submit"
  {disabled}
  {icon}
  {label}
  on:click={dispatchClick} />
