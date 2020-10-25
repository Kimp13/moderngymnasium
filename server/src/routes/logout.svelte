<div class="logout">
  <h2>
    Выходим, выходим, выходим!
  </h2>
  <h3>
    Наступят времена почище.
  </h3>
</div>

<script>
  import { deleteCookie } from "../../utils/cookies";
  import { onMount } from "svelte";

  onMount(() => {
    deleteCookie('jwt');

    window.location.href = '/auth';
  });
</script>

<style lang="sass">
  @import "../theme/colors"

  .logout
    margin: 0 auto
    padding: 3rem 0
    width: 90%
    max-width: 35rem
    
    h2,
    h3
      color: $color-green
      text-align: left
</style>

