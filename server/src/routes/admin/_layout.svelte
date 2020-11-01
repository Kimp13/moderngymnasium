<script context="module">
  export async function preload(page, session) {
    if (!session.user.isAuthenticated) {
      this.error(404, "Такой страницы, простите, не существует.");
    }

    return {};
  }
</script>

<script>
  import {
    AppBar,
    Button,
    Icon,
    NavigationDrawer,
    Overlay,
  } from "svelte-materialify/src";

  import { mdiMenu } from "@mdi/js";

  let drawerActive = false;

  const toggleDrawer = () => (drawerActive = !drawerActive);

  const closeNavigationDrawer = () => {
    drawerActive = false;
  };
</script>

<style lang="scss">
  header {
    position: sticky;
    top: 0;
    height: 3rem;
  }
</style>

<header>
  <AppBar>
    <div slot="icon">
      <Button fab depressed on:click={toggleDrawer}>
        <Icon path={mdiMenu} />
      </Button>
    </div>
    <span slot="title"> moderngymnasium </span>
  </AppBar>
</header>

<slot />
<NavigationDrawer absolute active={drawerActive}>
  <a on:click={closeNavigationDrawer} href="/admin/">Главная</a>
  <a on:click={closeNavigationDrawer} href="/admin/home">Дом админа</a>
  <a on:click={closeNavigationDrawer} href="/admin/hello">Hello админа</a>
</NavigationDrawer>
<Overlay active={drawerActive} absolute on:click={toggleDrawer} index={1} />
