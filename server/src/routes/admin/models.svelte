<script context="module">
  import getPermission from "getPermission";
  import { getPreloadApiResponse } from "requests";

  export async function preload(page, session) {
    const permission = getPermission(session.user.permissions, [
      "admin",
      "insert",
    ]);

    if (permission) {
      const models = await getPreloadApiResponse(
        "/admin/api/getModels",
        {},
        this
      );

      return {
        models,
      };
    } else {
      this.error(404, "Это не те дроиды");
    }
  }
</script>

<script>
  import Title from "Title.svelte";
  import Button from "Button.svelte";
  import { mdiPlusBox } from "@mdi/js";
  import { goto } from "@sapper/app";
  import {
    Card,
    CardTitle,
    CardSubtitle,
    CardActions,
  } from "svelte-materialify/src";

  export let models;
</script>

<style lang="scss">
  .cards {
    display: flex;
    flex-wrap: wrap;

    :global(.cards-element) {
      flex: 20rem 1 1;
      margin: 1rem;
    }
  }
</style>

<Title caption="Модели" />

<div class="cards">
  {#each models as model}
    <Card class="cards-element">
      <CardTitle>{model.tableName}</CardTitle>
      <CardActions>
        <Button
          on:click={() => goto(`/admin/insert/${model.tableName}`)}
          fab
          icon={mdiPlusBox}
          on:click={() => console.log('Hello!')} />
      </CardActions>
    </Card>
  {/each}
</div>
