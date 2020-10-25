<script>
  import { createEventDispatcher } from "svelte";

  import Button from "../Button.svelte";

  export let disabled = false;
  export let icon = "login";
  export let label = "Вход";

  const dispatch = createEventDispatcher();

  const dispatchClick = e => dispatch('click', e);
</script>

<Button
  {disabled}
  class="submit"
  variant="raised"
  {icon}
  {label}
  on:click={dispatchClick} />
