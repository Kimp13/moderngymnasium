<script>
  import { stores } from "@sapper/app";

  const { session } = stores();
</script>

<style lang="sass">
  h1
    color: #aaa
    text-align: center
</style>

<h1>Hello, {$session.user.username}!</h1>
